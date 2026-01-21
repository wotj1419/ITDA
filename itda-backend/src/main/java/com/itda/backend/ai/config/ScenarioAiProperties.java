package com.itda.backend.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.scenario")
public class ScenarioAiProperties {

    private String modelText;

    private long timeoutMs = 8000;
}
