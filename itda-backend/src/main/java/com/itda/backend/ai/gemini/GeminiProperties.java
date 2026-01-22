package com.itda.backend.ai.gemini;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.gemini")
public class GeminiProperties {

    /**
     * When true, return a stub image without calling the API.
     */
    private boolean stub = true;

    /**
     * Optional API key for REST calls. If blank, ADC is used.
     */
    private String apiKey;

    /**
     * Vertex AI image model id or full resource name.
     * Example: "imagen-3.0-generate-001" or
     * "projects/{project}/locations/{location}/publishers/google/models/{model}".
     */
    private String imageModel;

    /**
     * Sample count for image generation.
     */
    private int sampleCount = 1;

    /**
     * Optional aspect ratio, e.g. "1:1", "16:9".
     */
    private String aspectRatio;

    /**
     * API timeout in milliseconds.
     */
    private long timeoutMs = 60000;
}
