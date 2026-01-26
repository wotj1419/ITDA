package com.itda.backend.ai;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.genai.Client;
import com.google.genai.types.HttpOptions;
import com.itda.backend.ai.config.AiVertexConfig;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenAiClientProvider {

    private static final List<String> SCOPES =
            List.of("https://www.googleapis.com/auth/cloud-platform");

    private final AiVertexConfig vertexConfig;

    private volatile Client client;

    public Client getClient() {
        Client cached = client;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (client == null) {
                client = buildClient();
            }
            return client;
        }
    }

    private Client buildClient() {
        try {
            String projectId = vertexConfig.requireProjectId();
            String location = vertexConfig.requireLocation();
            String apiVersion = vertexConfig.getApiVersion();

            Client.Builder builder = Client.builder()
                    .project(projectId)
                    .location(location)
                    .vertexAI(true)
                    .credentials(loadDefaultCredentials());

            Optional.ofNullable(apiVersion)
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .ifPresent(value -> builder.httpOptions(HttpOptions.builder().apiVersion(value).build()));

            log.info(
                    "Gen AI client initialized (projectId={}, location={}, apiVersion={})",
                    projectId,
                    location,
                    apiVersion
            );
            return builder.build();
        } catch (IOException e) {
            throw new IllegalStateException("Vertex AI credentials not configured", e);
        }
    }

    private GoogleCredentials loadDefaultCredentials() throws IOException {
        return GoogleCredentials.getApplicationDefault().createScoped(SCOPES);
    }

    @PreDestroy
    public void close() {
        Client cached = client;
        if (cached == null) {
            return;
        }
        synchronized (this) {
            cached = client;
            client = null;
        }
        try {
            cached.close();
        } catch (Exception e) {
            log.warn("Failed to close Gen AI client", e);
        }
    }
}
