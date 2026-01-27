package com.itda.backend.ai.veo;

import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateVideosConfig;
import com.google.genai.types.GenerateVideosOperation;
import com.google.genai.types.GenerateVideosResponse;
import com.google.genai.types.GenerateVideosSource;
import com.google.genai.types.GetOperationConfig;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Image;
import com.google.genai.types.Video;
import com.itda.backend.ai.AiProviderException;
import com.itda.backend.ai.AiStubAssets;
import com.itda.backend.ai.GenAiClientProvider;
import com.itda.backend.ai.config.AiVertexConfig;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class VeoClient {

    private static final String DEFAULT_VIDEO_MIME = "video/mp4";
    private static final String DEFAULT_ASPECT_RATIO = "16:9";
    private static final long MIN_POLL_INTERVAL_MS = 200L;
    private static final int MAX_POLL_REQUEST_TIMEOUT_MS = 30_000;

    private static final String SETTING_PROVIDER = "provider";
    private static final String SETTING_DURATION_SECONDS = "duration";
    private static final String SETTING_ASPECT_RATIO = "aspectRatio";
    private static final String SETTING_CAMERA_MOTION = "cameraMotion";
    private static final String SETTING_MOTION_DESCRIPTION = "motionDescription";

    private static final Map<String, String> MODEL_ALIASES = Map.of(
            "VEO_3_1", "veo-3.1-generate-001",
            "VEO_3_1_FAST", "veo-3.1-fast-generate-001",
            "VEO_3_0", "veo-3.0-generate-001",
            "VEO_2_0", "veo-2.0-generate-001"
    );

    private final VeoProperties veoProperties;
    private final AiVertexConfig vertexConfig;
    private final GenAiClientProvider clientProvider;

    public VeoResult generateVideo(VeoRequest request) {
        String prompt = requirePrompt(request);
        if (veoProperties.isStub()) {
            log.info("[VeoClient] Stub enabled. Returning stub video.");
            return new VeoResult(AiStubAssets.stubMp4Bytes(), DEFAULT_VIDEO_MIME);
        }
        ensureConfig();
        VeoSettings settings = VeoSettings.from(request.settings());
        String model = resolveModel(settings);
        long timeoutMs = resolveTimeoutMs();

        log.info(
                "[VeoClient] generateVideo start: model={}, projectId={}, location={}, timeoutMs={}",
                model,
                vertexConfig.getProjectId(),
                vertexConfig.getLocation(),
                timeoutMs
        );

        Image firstFrame = buildImage(request.firstFrame());
        if (request.lastFrame() != null) {
            // TODO(P1): support lastFrame when Veo SDK/REST input is 확정되면 추가
            log.debug("[VeoClient] lastFrame provided but ignored (P1): contentType={}", request.lastFrame().contentType());
        }
        GenerateVideosSource.Builder sourceBuilder = GenerateVideosSource.builder()
                .prompt(buildPrompt(prompt, settings));
        if (firstFrame != null) {
            sourceBuilder.image(firstFrame);
        }
        GenerateVideosSource source = sourceBuilder.build();
        GenerateVideosConfig config = buildRequestConfig(settings, timeoutMs);

        try {
            var client = clientProvider.getClient();
            GenerateVideosOperation operation = client.models.generateVideos(model, source, config);
            GenerateVideosOperation completed = pollOperation(operation, timeoutMs);
            return parseOperationResult(completed);
        } catch (ApiException e) {
            throw buildApiException("VEO_CALL_FAILED", e);
        } catch (GenAiIOException e) {
            throw new AiProviderException("VEO_CALL_FAILED: " + e.getMessage(), e);
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("VEO_CALL_FAILED: " + e.getMessage(), e);
        }
    }

    private GenerateVideosOperation pollOperation(GenerateVideosOperation operation, long timeoutMs) {
        long deadline = System.currentTimeMillis() + normalizeTimeoutMs(timeoutMs, VeoProperties.DEFAULT_TIMEOUT_MS);
        GenerateVideosOperation current = operation;
        long pollIntervalMs = Math.max(MIN_POLL_INTERVAL_MS, veoProperties.getPollIntervalMs());

        while (System.currentTimeMillis() < deadline) {
            try {
                long remainingMs = Math.max(1, deadline - System.currentTimeMillis());
                long requestTimeoutMs = Math.min(MAX_POLL_REQUEST_TIMEOUT_MS, remainingMs);
                GetOperationConfig config = buildGetOperationConfig(requestTimeoutMs);
                current = clientProvider.getClient().operations.getVideosOperation(current, config);
                if (current.done().orElse(false)) {
                    if (current.error().isPresent()) {
                        throw new AiProviderException(buildOperationErrorSummary(current.error().get()));
                    }
                    return current;
                }
                Thread.sleep(pollIntervalMs);
            } catch (AiProviderException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AiProviderException("VEO_CALL_FAILED: operation polling interrupted", e);
            } catch (Exception e) {
                throw new AiProviderException("VEO_CALL_FAILED: " + e.getMessage(), e);
            }
        }
        throw new AiProviderException("VEO_CALL_FAILED: operation timed out");
    }

    private GetOperationConfig buildGetOperationConfig(long requestTimeoutMs) {
        return GetOperationConfig.builder()
                .httpOptions(HttpOptions.builder().timeout(toIntTimeoutMs(requestTimeoutMs, MAX_POLL_REQUEST_TIMEOUT_MS)).build())
                .build();
    }

    private VeoResult parseOperationResult(GenerateVideosOperation operation) {
        GenerateVideosResponse response = operation.response().orElse(null);
        if (response == null) {
            throw new AiProviderException("VEO_CALL_FAILED: missing response");
        }

        for (var generated : response.generatedVideos().orElse(java.util.List.of())) {
            Video video = generated.video().orElse(null);
            if (video == null) {
                continue;
            }
            byte[] bytes = video.videoBytes().orElse(null);
            if (bytes != null && bytes.length > 0) {
                String mime = video.mimeType().orElse(DEFAULT_VIDEO_MIME);
                return new VeoResult(bytes, mime);
            }
            String uri = video.uri().orElse(null);
            if (uri != null && !uri.isBlank()) {
                throw new AiProviderException("VEO_CALL_FAILED: video bytes missing (uri returned)");
            }
        }
        Optional<java.util.List<String>> filteredReasons = response.raiMediaFilteredReasons();
        if (filteredReasons.isPresent() && !filteredReasons.get().isEmpty()) {
            throw new AiProviderException("VEO_CALL_FAILED: filtered=" + filteredReasons.get());
        }
        throw new AiProviderException("VEO_CALL_FAILED: video payload missing");
    }

    private GenerateVideosConfig buildRequestConfig(VeoSettings settings, long timeoutMs) {
        GenerateVideosConfig.Builder builder = GenerateVideosConfig.builder()
                .numberOfVideos(1)
                .httpOptions(HttpOptions.builder().timeout(toIntTimeoutMs(timeoutMs, VeoProperties.DEFAULT_TIMEOUT_MS)).build());

        Integer durationSeconds = settings.durationSeconds();
        if (durationSeconds != null) {
            builder.durationSeconds(durationSeconds);
        }
        builder.aspectRatio(settings.aspectRatioOrDefault(DEFAULT_ASPECT_RATIO));
        return builder.build();
    }

    private Image buildImage(VeoImage image) {
        if (image == null || image.bytes() == null || image.bytes().length == 0) {
            return null;
        }
        String contentType = normalizeImageMime(image.contentType());
        return Image.builder()
                .imageBytes(image.bytes())
                .mimeType(contentType)
                .build();
    }

    private String normalizeImageMime(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "image/png";
        }
        String trimmed = contentType.trim();
        return trimmed.startsWith("image/") ? trimmed : "image/png";
    }

    private String buildPrompt(String prompt, VeoSettings settings) {
        String cameraMotion = settings.cameraMotion();
        String motionDescription = settings.motionDescription();
        if (cameraMotion == null && motionDescription == null) {
            return prompt;
        }
        StringBuilder builder = new StringBuilder(prompt);
        if (cameraMotion != null) {
            builder.append(" Camera motion: ").append(cameraMotion).append(".");
        }
        if (motionDescription != null) {
            builder.append(" Motion description: ").append(motionDescription).append(".");
        }
        return builder.toString();
    }

    private String resolveModel(VeoSettings settings) {
        String provider = settings.provider();
        if (provider == null) {
            return veoProperties.getModel();
        }
        String normalized = provider.trim().toUpperCase(Locale.ROOT);
        String alias = MODEL_ALIASES.get(normalized);
        if (alias != null) {
            return alias;
        }
        String trimmed = provider.trim();
        return trimmed.toLowerCase(Locale.ROOT).startsWith("veo-") ? trimmed : veoProperties.getModel();
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

    private String buildOperationErrorSummary(Map<String, Object> error) {
        if (error == null || error.isEmpty()) {
            return "VEO_CALL_FAILED: operation error";
        }
        Object code = error.get("code");
        Object status = error.get("status");
        Object message = error.get("message");
        StringBuilder summary = new StringBuilder("VEO_CALL_FAILED");
        if (code != null) {
            summary.append(": ").append(code);
        }
        if (status != null) {
            summary.append(" ").append(status);
        }
        if (message != null) {
            summary.append(" ").append(message);
        }
        return summary.toString();
    }

    private String requirePrompt(VeoRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is empty");
        }
        return request.prompt().trim();
    }

    private void ensureConfig() {
        if (veoProperties.getModel() == null || veoProperties.getModel().isBlank()) {
            throw new IllegalStateException("Veo configuration missing (model)");
        }
        vertexConfig.requireProjectId();
        vertexConfig.requireLocation();
    }

    private long resolveTimeoutMs() {
        return normalizeTimeoutMs(veoProperties.getTimeoutMs(), VeoProperties.DEFAULT_TIMEOUT_MS);
    }

    private long normalizeTimeoutMs(long timeoutMs, long defaultMs) {
        return timeoutMs > 0 ? timeoutMs : defaultMs;
    }

    private int toIntTimeoutMs(long timeoutMs, long defaultMs) {
        long normalized = normalizeTimeoutMs(timeoutMs, defaultMs);
        if (normalized > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) normalized;
    }

    private static final class VeoSettings {
        private final Map<String, Object> raw;

        private VeoSettings(Map<String, Object> raw) {
            this.raw = raw == null ? Map.of() : raw;
        }

        private static VeoSettings from(Map<String, Object> raw) {
            return new VeoSettings(raw);
        }

        private String provider() {
            return readString(SETTING_PROVIDER);
        }

        private Integer durationSeconds() {
            return readInt(SETTING_DURATION_SECONDS);
        }

        private String aspectRatioOrDefault(String defaultValue) {
            String value = readString(SETTING_ASPECT_RATIO);
            return value == null ? defaultValue : value;
        }

        private String cameraMotion() {
            return readString(SETTING_CAMERA_MOTION);
        }

        private String motionDescription() {
            return readString(SETTING_MOTION_DESCRIPTION);
        }

        private Integer readInt(String key) {
            Object value = raw.get(key);
            if (value == null) {
                return null;
            }
            if (value instanceof Number number) {
                return number.intValue();
            }
            try {
                return Integer.parseInt(value.toString());
            } catch (NumberFormatException e) {
                return null;
            }
        }

        private String readString(String key) {
            Object value = raw.get(key);
            if (value == null) {
                return null;
            }
            String text = value.toString().trim();
            return text.isEmpty() ? null : text;
        }
    }
}
