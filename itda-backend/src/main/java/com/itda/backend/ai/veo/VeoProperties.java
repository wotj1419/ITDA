package com.itda.backend.ai.veo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.veo")
public class VeoProperties {

    public static final String DEFAULT_MODEL = "veo-3.1-generate-001";
    public static final long DEFAULT_TIMEOUT_MS = 120_000L;
    public static final long DEFAULT_POLL_INTERVAL_MS = 2_000L;

    private String model = DEFAULT_MODEL;

    private long timeoutMs = DEFAULT_TIMEOUT_MS;

    private long pollIntervalMs = DEFAULT_POLL_INTERVAL_MS;

    /**
     * Stub mode (no external calls). Enable only when explicit fallback is needed.
     */
    private boolean stub = false;
}
