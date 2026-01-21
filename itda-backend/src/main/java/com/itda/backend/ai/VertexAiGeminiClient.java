package com.itda.backend.ai;

import com.google.api.gax.rpc.ApiException;
import com.google.api.gax.rpc.StatusCode;
import com.google.cloud.vertexai.VertexAI;
import com.google.cloud.vertexai.api.Candidate;
import com.google.cloud.vertexai.api.GenerateContentResponse;
import com.google.cloud.vertexai.generativeai.GenerativeModel;
import com.itda.backend.ai.config.ScenarioAiProperties;
import com.itda.backend.ai.config.VertexAiGeminiProperties;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@Component
@RequiredArgsConstructor
public class VertexAiGeminiClient {

    private static final long DEFAULT_TIMEOUT_MS = 8000;

    private final VertexAiGeminiProperties vertexAiProperties;
    private final ScenarioAiProperties scenarioAiProperties;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(Math.max(2, Runtime.getRuntime().availableProcessors()));

    public TextGenerationResponse generate(TextGenerationRequest request) {
        String prompt = requirePrompt(request);
        String model = resolveModel(request);
        ensureVertexConfig();

        return executeWithTimeout(prompt, model, resolveTimeoutMs());
    }

    private TextGenerationResponse doGenerate(String prompt, String model) {
        try (VertexAI vertexAI = new VertexAI(vertexAiProperties.getProjectId(), vertexAiProperties.getLocation())) {
            GenerativeModel generativeModel = new GenerativeModel(model, vertexAI);
            GenerateContentResponse response = generativeModel.generateContent(prompt);
            String text = extractText(response);
            if (text == null || text.isBlank()) {
                throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Empty response from AI provider");
            }
            return new TextGenerationResponse(text.trim());
        } catch (ApiException apiException) {
            throw mapApiException(apiException);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null || response.getCandidatesCount() == 0) {
            return null;
        }
        Candidate candidate = response.getCandidates(0);
        if (candidate == null || candidate.getContent() == null) {
            return null;
        }
        return candidate.getContent().getPartsList().stream()
                .map(part -> part == null ? null : part.getText())
                .filter(text -> text != null && !text.isBlank())
                .findFirst()
                .orElse(null);
    }

    private BusinessException mapApiException(ApiException apiException) {
        StatusCode.Code code = apiException.getStatusCode().getCode();
        log.warn("Vertex AI error: status={} message={}", code, apiException.getMessage());

        if (code == StatusCode.Code.UNAUTHENTICATED || code == StatusCode.Code.PERMISSION_DENIED) {
            return new BusinessException(ErrorCode.AI_AUTH_FAILED, apiException);
        }
        if (code == StatusCode.Code.RESOURCE_EXHAUSTED) {
            return new BusinessException(ErrorCode.AI_RATE_LIMITED, apiException);
        }
        if (code == StatusCode.Code.DEADLINE_EXCEEDED) {
            return new BusinessException(ErrorCode.AI_TIMEOUT, apiException);
        }
        if (code == StatusCode.Code.UNAVAILABLE) {
            return new BusinessException(ErrorCode.AI_PROVIDER_ERROR, apiException);
        }
        return new BusinessException(ErrorCode.INTERNAL_ERROR, apiException);
    }

    private TextGenerationResponse executeWithTimeout(String prompt, String model, long timeoutMs) {
        Future<TextGenerationResponse> future = executor.submit(() -> doGenerate(prompt, model));
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BusinessException(ErrorCode.AI_TIMEOUT, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            if (cause instanceof ApiException apiException) {
                throw mapApiException(apiException);
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private String requirePrompt(TextGenerationRequest request) {
        return Optional.ofNullable(request.prompt())
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
    }

    private String resolveModel(TextGenerationRequest request) {
        String model = Optional.ofNullable(request.model())
                .filter(value -> !value.isBlank())
                .orElseGet(scenarioAiProperties::getModelText);

        if (model == null || model.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "AI model is not configured");
        }
        return model;
    }

    private long resolveTimeoutMs() {
        long timeoutMs = scenarioAiProperties.getTimeoutMs();
        return timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }

    private void ensureVertexConfig() {
        if (vertexAiProperties.getProjectId() == null || vertexAiProperties.getProjectId().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "GCP project id is not configured");
        }
        if (vertexAiProperties.getLocation() == null || vertexAiProperties.getLocation().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "GCP location is not configured");
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
