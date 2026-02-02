package com.itda.backend.ai.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiImageProperties {

    private static final long DEFAULT_TIMEOUT_MS = 180_000;

    private String imageModel = "gemini-2.5-flash-image";

    private long timeoutMs = DEFAULT_TIMEOUT_MS;

    /**
     * Stub mode (no external calls). Enable only when explicit fallback is needed.
     */
    private boolean stub = false;

    public String resolvedImageModel() {
        if (imageModel == null) {
            return null;
        }
        String value = imageModel.trim();
        return value.isBlank() ? null : value;
    }

    public long resolvedTimeoutMs() {
        return timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
    }
}
