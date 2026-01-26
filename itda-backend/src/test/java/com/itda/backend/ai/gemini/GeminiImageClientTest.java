package com.itda.backend.ai.gemini;

import com.itda.backend.ai.GenAiClientProvider;
import com.itda.backend.ai.config.AiVertexConfig;
import com.itda.backend.ai.config.AiVertexProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiImageClientTest {

    @Test
    void generateImage_stubEnabled_returnsPng() {
        GeminiImageProperties imageProperties = new GeminiImageProperties();
        imageProperties.setStub(true);

        AiVertexConfig vertexConfig = new AiVertexConfig(new AiVertexProperties());
        GenAiClientProvider clientProvider = Mockito.mock(GenAiClientProvider.class);

        GeminiImageClient client = new GeminiImageClient(imageProperties, vertexConfig, clientProvider);

        GeminiImageResult result = client.generateImage("test prompt", null);

        assertThat(result).isNotNull();
        assertThat(result.bytes()).isNotEmpty();
        assertThat(result.contentType()).isEqualTo("image/png");
        Mockito.verifyNoInteractions(clientProvider);
    }
}
