package com.itda.backend.ai.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiVertexConfig {

    private final AiVertexProperties properties;

    public String getProjectId() {
        return normalize(properties.getProjectId());
    }

    public String getLocation() {
        return normalize(properties.getLocation());
    }

    public String getApiVersion() {
        return normalize(properties.getApiVersion());
    }

    public String requireProjectId() {
        return require("projectId", getProjectId());
    }

    public String requireLocation() {
        return require("location", getLocation());
    }

    private String require(String label, String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Vertex AI configuration missing (" + label + ")");
        }
        return value;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
