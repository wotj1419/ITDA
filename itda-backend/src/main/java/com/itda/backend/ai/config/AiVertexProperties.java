package com.itda.backend.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "ai.vertex")
public class AiVertexProperties {

    private String projectId;

    private String location;

    private String apiVersion = "v1";
}
