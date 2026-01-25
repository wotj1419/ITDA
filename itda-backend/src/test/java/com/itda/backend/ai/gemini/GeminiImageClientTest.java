package com.itda.backend.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.ai.config.VertexAiGeminiProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiImageClientTest {

    @Test
    void generateImage_stubEnabled_returnsPng() {
        VertexAiGeminiProperties vertexProps = new VertexAiGeminiProperties();
        GeminiProperties geminiProps = new GeminiProperties();
        geminiProps.setStub(true);

        GeminiImageClient client = new GeminiImageClient(vertexProps, geminiProps, new ObjectMapper());

        GeminiImageResult result = client.generateImage("test prompt");

        assertThat(result).isNotNull();
        assertThat(result.bytes()).isNotEmpty();
        assertThat(result.contentType()).isEqualTo("image/png");
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_GEMINI_INTEGRATION", matches = "true")
    void generateImage_realCall_returnsBytes() {
        String projectId = System.getenv("GCP_PROJECT_ID");
        String location = System.getenv("GCP_LOCATION");
        String model = System.getenv("GEMINI_IMAGE_MODEL");

        Assumptions.assumeTrue(projectId != null && !projectId.isBlank());
        Assumptions.assumeTrue(location != null && !location.isBlank());
        Assumptions.assumeTrue(model != null && !model.isBlank());

        VertexAiGeminiProperties vertexProps = new VertexAiGeminiProperties();
        vertexProps.setProjectId(projectId);
        vertexProps.setLocation(location);

        GeminiProperties geminiProps = new GeminiProperties();
        geminiProps.setStub(false);
        geminiProps.setImageModel(model);
        geminiProps.setApiKey(System.getenv("GEMINI_API_KEY"));
        geminiProps.setTimeoutMs(60000);
        geminiProps.setSampleCount(1);

        GeminiImageClient client = new GeminiImageClient(vertexProps, geminiProps, new ObjectMapper());

        GeminiImageResult result = client.generateImage("test prompt");

        assertThat(result).isNotNull();
        assertThat(result.bytes()).isNotEmpty();
    }
}
