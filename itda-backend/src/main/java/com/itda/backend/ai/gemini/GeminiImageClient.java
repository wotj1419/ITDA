package com.itda.backend.ai.gemini;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.ImageConfig;
import com.google.genai.types.Part;
import com.itda.backend.ai.AiProviderException;
import com.itda.backend.ai.AiStubAssets;
import com.itda.backend.ai.GenAiClientProvider;
import com.itda.backend.ai.config.AiVertexConfig;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GeminiImageClient {

    private static final String DEFAULT_IMAGE_MIME = "image/png";
    private static final String ERROR_PREFIX = "GEMINI_CALL_FAILED";
    private static final List<String> RESPONSE_MODALITIES = List.of("TEXT", "IMAGE");
    private static final long DEFAULT_TIMEOUT_MS = 60_000;

    private final GeminiImageProperties imageProperties;
    private final AiVertexConfig vertexConfig;
    private final GenAiClientProvider clientProvider;

    public GeminiImageResult generateImage(String prompt, Map<String, Object> settings) {
        return generateImage(prompt, settings, List.of());
    }

    public GeminiImageResult generateImage(
            String prompt,
            Map<String, Object> settings,
            List<ReferenceImage> referenceImages
    ) {
        String resolvedPrompt = requirePrompt(prompt);
        if (imageProperties.isStub()) {
            log.info("[GeminiImageClient] Stub enabled. Returning stub image.");
            return new GeminiImageResult(AiStubAssets.stubPngBytes(), DEFAULT_IMAGE_MIME);
        }
        String model = requireImageModel();
        ensureVertexConfig();
        log.info("[GeminiImageClient] generateImage start: model={}", model);
        if (log.isDebugEnabled()) {
            log.debug(
                    "[GeminiImageClient] vertex config: projectId={}, location={}",
                    vertexConfig.getProjectId(),
                    vertexConfig.getLocation()
            );
        }
        GenerateContentConfig config = buildRequestConfig(settings);

        try {
            GenerateContentResponse response = generateContent(model, resolvedPrompt, referenceImages, config);
            logResponseDebug(response);
            return parseResponse(response);
        } catch (ApiException e) {
            throw buildApiException(ERROR_PREFIX, e);
        } catch (GenAiIOException e) {
            throw new AiProviderException(ERROR_PREFIX + ": " + safeMessage(e), e);
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(ERROR_PREFIX + ": " + safeMessage(e), e);
        }
    }

    private GenerateContentResponse generateContent(
            String model,
            String prompt,
            List<ReferenceImage> referenceImages,
            GenerateContentConfig config
    ) {
        if (referenceImages == null || referenceImages.isEmpty()) {
            return clientProvider.getClient()
                    .models
                    .generateContent(model, prompt, config);
        }
        Content content = buildContent(prompt, referenceImages);
        return clientProvider.getClient()
                .models
                .generateContent(model, content, config);
    }

    private Content buildContent(String prompt, List<ReferenceImage> referenceImages) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText(prompt));
        for (ReferenceImage referenceImage : referenceImages) {
            if (referenceImage == null || referenceImage.bytes() == null || referenceImage.bytes().length == 0) {
                throw new AiProviderException(ERROR_PREFIX + ": empty reference image");
            }
            String mimeType = referenceImage.mimeType() == null || referenceImage.mimeType().isBlank()
                    ? DEFAULT_IMAGE_MIME
                    : referenceImage.mimeType();
            parts.add(Part.fromBytes(referenceImage.bytes(), mimeType));
        }
        return Content.fromParts(parts.toArray(new Part[0]));
    }

    private GenerateContentConfig buildRequestConfig(Map<String, Object> settings) {
        long timeoutMs = imageProperties.resolvedTimeoutMs();
        GenerateContentConfig.Builder builder = GenerateContentConfig.builder()
                .responseModalities(RESPONSE_MODALITIES)
                .httpOptions(HttpOptions.builder().timeout(toIntTimeoutMs(timeoutMs)).build());

        String aspectRatio = readString(settings, "aspectRatio");
        if (aspectRatio != null) {
            builder.imageConfig(ImageConfig.builder().aspectRatio(aspectRatio).build());
        }
        return builder.build();
    }

    private GeminiImageResult parseResponse(GenerateContentResponse response) {
        if (response == null) {
            throw new AiProviderException(ERROR_PREFIX + ": empty response");
        }
        if (response.parts() == null || response.parts().isEmpty()) {
            throw new AiProviderException(ERROR_PREFIX + ": empty parts");
        }
        for (var part : response.parts()) {
            Blob blob = part.inlineData().orElse(null);
            if (blob == null) {
                continue;
            }
            byte[] data = blob.data().orElse(null);
            if (data == null || data.length == 0) {
                continue;
            }
            String mimeType = blob.mimeType().orElse(DEFAULT_IMAGE_MIME);
            return new GeminiImageResult(data, mimeType);
        }
        throw new AiProviderException(ERROR_PREFIX + ": image payload missing");
    }

    private void logResponseDebug(GenerateContentResponse response) {
        if (!log.isDebugEnabled()) {
            return;
        }
        if (response == null) {
            log.debug("[GeminiImageClient] response is null");
            return;
        }
        String text = response.text();
        int partsCount = response.parts() == null ? 0 : response.parts().size();
        log.debug(
                "[GeminiImageClient] response: hasText={}, textLength={}, partsCount={}",
                text != null && !text.isBlank(),
                text == null ? 0 : text.length(),
                partsCount
        );
        if (response.parts() == null) {
            return;
        }
        for (int i = 0; i < response.parts().size(); i++) {
            var part = response.parts().get(i);
            boolean hasInline = part.inlineData().isPresent();
            int inlineSize = part.inlineData().flatMap(b -> b.data()).map(d -> d.length).orElse(0);
            String mime = part.inlineData().flatMap(b -> b.mimeType()).orElse("none");
            boolean hasText = part.text().isPresent() && !part.text().orElse("").isBlank();
            log.debug(
                    "[GeminiImageClient] part[{}]: hasInlineData={}, inlineBytes={}, mime={}, hasText={}",
                    i,
                    hasInline,
                    inlineSize,
                    mime,
                    hasText
            );
        }
    }

    private AiProviderException buildApiException(String prefix, ApiException exception) {
        StringBuilder summary = new StringBuilder(prefix)
                .append(": ")
                .append(exception.code());
        if (exception.status() != null && !exception.status().isBlank()) {
            summary.append(" ").append(exception.status());
        }
        if (exception.message() != null && !exception.message().isBlank()) {
            summary.append(" ").append(exception.message());
        }
        return new AiProviderException(summary.toString(), exception);
    }

    private String requirePrompt(String prompt) {
        return Optional.ofNullable(prompt)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is empty"));
    }

    private String requireImageModel() {
        String model = imageProperties.resolvedImageModel();
        if (model == null) {
            throw new IllegalStateException("Vertex AI configuration missing (imageModel)");
        }
        return model;
    }

    private void ensureVertexConfig() {
        vertexConfig.requireProjectId();
        vertexConfig.requireLocation();
    }

    private int toIntTimeoutMs(long timeoutMs) {
        if (timeoutMs <= 0) {
            return (int) DEFAULT_TIMEOUT_MS;
        }
        if (timeoutMs > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) timeoutMs;
    }

    private String readString(Map<String, Object> settings, String key) {
        if (settings == null || key == null) {
            return null;
        }
        Object value = settings.get(key);
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }

    private String safeMessage(Throwable t) {
        if (t == null) {
            return "unknown error";
        }
        String message = t.getMessage();
        return message == null || message.isBlank() ? t.getClass().getSimpleName() : message.trim();
    }
}
