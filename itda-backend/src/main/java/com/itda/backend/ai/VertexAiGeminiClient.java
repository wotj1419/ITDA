package com.itda.backend.ai;

import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.itda.backend.ai.config.AiVertexConfig;
import com.itda.backend.ai.config.ScenarioAiProperties;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class VertexAiGeminiClient {

    private static final long DEFAULT_TIMEOUT_MS = 180000;
    private static final long SHUTDOWN_TIMEOUT_MS = 2000;

    private final GenAiClientProvider clientProvider;
    private final AiVertexConfig vertexConfig;
    private final ScenarioAiProperties scenarioAiProperties;

    private final AtomicInteger threadCounter = new AtomicInteger(0);
    private final ExecutorService executor =
            Executors.newFixedThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors()),
                    threadFactory()
            );

    public TextGenerationResponse generate(TextGenerationRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        String prompt = requirePrompt(request.prompt());
        String model = resolveModel(request.model());
        long timeoutMs = resolveTimeoutMs();
        ensureVertexConfig();

        return executeWithTimeout(() -> doGenerate(model, prompt, timeoutMs), timeoutMs);
    }

    private TextGenerationResponse doGenerate(String model, String prompt, long timeoutMs) {
        try {
            GenerateContentConfig config = buildConfig(timeoutMs);
            GenerateContentResponse response = clientProvider.getClient()
                    .models
                    .generateContent(model, prompt, config);
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

    private GenerateContentConfig buildConfig(long timeoutMs) {
        return GenerateContentConfig.builder()
                .httpOptions(HttpOptions.builder().timeout(toIntTimeoutMs(timeoutMs)).build())
                .build();
    }

    private String extractText(GenerateContentResponse response) {
        if (response == null) {
            return null;
        }
        String text = response.text();
        if (text != null && !text.isBlank()) {
            return text;
        }
        if (response.parts() == null || response.parts().isEmpty()) {
            return null;
        }
        return response.parts().stream()
                .map(part -> part.text().orElse(null))
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private BusinessException mapApiException(ApiException apiException) {
        int code = apiException.code();
        log.warn("Vertex AI error: status={} code={} message={}", apiException.status(), code, apiException.message());

        if (code == 401 || code == 403) {
            return new BusinessException(ErrorCode.AI_AUTH_FAILED, apiException);
        }
        if (code == 429) {
            return new BusinessException(ErrorCode.AI_RATE_LIMITED, apiException);
        }
        if (code == 408 || code == 504) {
            return new BusinessException(ErrorCode.AI_TIMEOUT, apiException);
        }
        if (code >= 500) {
            return new BusinessException(ErrorCode.AI_PROVIDER_ERROR, apiException);
        }
        return new BusinessException(ErrorCode.INTERNAL_ERROR, apiException);
    }

    private TextGenerationResponse executeWithTimeout(Callable<TextGenerationResponse> task, long timeoutMs) {
        Future<TextGenerationResponse> future = executor.submit(task);
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BusinessException(ErrorCode.AI_TIMEOUT, e);
        } catch (java.util.concurrent.ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException businessException) {
                throw businessException;
            }
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, cause == null ? e : cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private String requirePrompt(String prompt) {
        return Optional.ofNullable(prompt)
                .map(String::trim)
                .filter(text -> !text.isEmpty())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REQUEST));
    }

    private String resolveModel(String modelOverride) {
        String model = Optional.ofNullable(modelOverride)
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

    private int toIntTimeoutMs(long timeoutMs) {
        if (timeoutMs <= 0) {
            return (int) DEFAULT_TIMEOUT_MS;
        }
        if (timeoutMs > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) timeoutMs;
    }

    private void ensureVertexConfig() {
        vertexConfig.requireProjectId();
        vertexConfig.requireLocation();
    }

    private ThreadFactory threadFactory() {
        ThreadFactory delegate = Executors.defaultThreadFactory();
        return runnable -> {
            Thread thread = delegate.newThread(runnable);
            thread.setName("vertex-ai-gemini-" + threadCounter.incrementAndGet());
            return thread;
        };
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
