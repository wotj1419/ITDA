package com.itda.backend.ai.gemini;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.itda.backend.ai.config.VertexAiGeminiProperties;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class GeminiImageClient {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final String DEFAULT_CONTENT_TYPE = "image/png";
    private static final byte[] STUB_PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAA" +
            "AAC0lEQVR4nGNgYAAAAAMAASsJTYQAAAAASUVORK5CYII="
    );
    private static final List<String> BASE64_KEYS = List.of(
            "bytesBase64Encoded",
            "bytes",
            "image",
            "data",
            "b64_json"
    );
    private static final List<String> MIME_KEYS = List.of(
            "mimeType",
            "mime_type",
            "contentType",
            "content_type"
    );

    private final VertexAiGeminiProperties vertexAiProperties;
    private final GeminiProperties geminiProperties;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    private volatile GoogleCredentials credentials;

    public GeminiImageClient(VertexAiGeminiProperties vertexAiProperties,
                             GeminiProperties geminiProperties,
                             ObjectMapper objectMapper) {
        this.vertexAiProperties = vertexAiProperties;
        this.geminiProperties = geminiProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = createRestTemplate(geminiProperties.getTimeoutMs());
    }

    public GeminiImageResult generateImage(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is required");
        }

        String model = requireNonBlank(geminiProperties.getImageModel(), "AI image model is not configured");
        boolean geminiModel = isGeminiModel(model);

        log.info("[GeminiImageClient] stub={}, model={}, projectId={}, location={}",
                geminiProperties.isStub(),
                model,
                safeTrim(vertexAiProperties.getProjectId()),
                safeTrim(vertexAiProperties.getLocation()));

        if (geminiProperties.isStub()) {
            log.warn("[GeminiImageClient] Stub mode enabled. Returning placeholder image.");
            return new GeminiImageResult(STUB_PNG_BYTES, DEFAULT_CONTENT_TYPE);
        }

        String endpoint = geminiModel
                ? buildGenerateContentEndpoint(model)
                : buildPredictEndpoint(model);
        String payload = geminiModel
                ? buildGenerateContentPayload(prompt)
                : buildPredictPayload(prompt);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String apiKey = safeTrim(geminiProperties.getApiKey());
        if (apiKey != null) {
            endpoint = appendApiKey(endpoint, apiKey);
        } else {
            try {
                headers.setBearerAuth(resolveAccessToken());
            } catch (IOException e) {
                throw new BusinessException(ErrorCode.AI_AUTH_FAILED, e);
            }
        }

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(endpoint, HttpMethod.POST, entity, String.class);
            return geminiModel
                    ? parseGenerateContentResponse(response.getBody())
                    : parsePredictResponse(response.getBody());
        } catch (HttpStatusCodeException e) {
            throw mapHttpException(e);
        } catch (ResourceAccessException e) {
            throw new BusinessException(ErrorCode.AI_TIMEOUT, e);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private RestTemplate createRestTemplate(long timeoutMs) {
        int timeout = (int) Math.min(Integer.MAX_VALUE, Math.max(1000L, timeoutMs));
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    private String buildPredictEndpoint(String model) {
        String projectId = requireNonBlank(vertexAiProperties.getProjectId(), "GCP project id is not configured");
        String location = requireNonBlank(vertexAiProperties.getLocation(), "GCP location is not configured");

        if (model.startsWith("projects/")) {
            return "https://" + location + "-aiplatform.googleapis.com/v1/" + model + ":predict";
        }

        String modelPath = model;
        if (!model.startsWith("publishers/") && !model.startsWith("models/")) {
            modelPath = "publishers/google/models/" + model;
        }

        return "https://" + location + "-aiplatform.googleapis.com/v1/projects/" + projectId +
                "/locations/" + location + "/" + modelPath + ":predict";
    }

    private String buildGenerateContentEndpoint(String model) {
        String projectId = requireNonBlank(vertexAiProperties.getProjectId(), "GCP project id is not configured");
        String location = requireNonBlank(vertexAiProperties.getLocation(), "GCP location is not configured");

        if (model.startsWith("projects/")) {
            return "https://" + location + "-aiplatform.googleapis.com/v1/" + model + ":generateContent";
        }

        String modelPath = model;
        if (!model.startsWith("publishers/") && !model.startsWith("models/")) {
            modelPath = "publishers/google/models/" + model;
        }

        return "https://" + location + "-aiplatform.googleapis.com/v1/projects/" + projectId +
                "/locations/" + location + "/" + modelPath + ":generateContent";
    }

    private String appendApiKey(String endpoint, String apiKey) {
        if (endpoint.contains("?")) {
            return endpoint + "&key=" + apiKey;
        }
        return endpoint + "?key=" + apiKey;
    }

    private String buildPredictPayload(String prompt) {
        Map<String, Object> instance = new LinkedHashMap<>();
        instance.put("prompt", prompt);

        Map<String, Object> parameters = new LinkedHashMap<>();
        if (geminiProperties.getSampleCount() > 0) {
            parameters.put("sampleCount", geminiProperties.getSampleCount());
        }
        String aspectRatio = safeTrim(geminiProperties.getAspectRatio());
        if (aspectRatio != null) {
            parameters.put("aspectRatio", aspectRatio);
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("instances", List.of(instance));
        if (!parameters.isEmpty()) {
            payload.put("parameters", parameters);
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private String buildGenerateContentPayload(String prompt) {
        Map<String, Object> part = new LinkedHashMap<>();
        part.put("text", prompt);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("role", "user");
        content.put("parts", List.of(part));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("contents", List.of(content));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        if (geminiProperties.getSampleCount() > 0) {
            generationConfig.put("candidateCount", geminiProperties.getSampleCount());
        }
        List<String> responseModalities = geminiProperties.getResponseModalities();
        if (responseModalities != null && !responseModalities.isEmpty()) {
            generationConfig.put("responseModalities", responseModalities);
        }
        String responseMimeType = safeTrim(geminiProperties.getResponseMimeType());
        if (responseMimeType != null) {
            generationConfig.put("responseMimeType", responseMimeType);
        }
        if (!generationConfig.isEmpty()) {
            payload.put("generationConfig", generationConfig);
        }

        String aspectRatio = safeTrim(geminiProperties.getAspectRatio());
        if (aspectRatio != null) {
            log.info("[GeminiImageClient] aspectRatio is ignored for Gemini generateContent: {}", aspectRatio);
        }

        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private GeminiImageResult parsePredictResponse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Empty response from image provider");
        }

        Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
        Object predictionsObj = response.get("predictions");
        if (!(predictionsObj instanceof List<?> predictions) || predictions.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Missing predictions in response");
        }

        Object first = predictions.get(0);
        if (first instanceof Map<?, ?> map) {
            String base64 = findString(map, BASE64_KEYS);
            String mimeType = findString(map, MIME_KEYS);

            if (base64 == null && map.containsKey("image") && map.get("image") instanceof Map<?, ?> nested) {
                base64 = findString(nested, BASE64_KEYS);
                if (mimeType == null) {
                    mimeType = findString(nested, MIME_KEYS);
                }
            }

            if (base64 == null) {
                throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Image bytes not found in response");
            }

            byte[] bytes = Base64.getDecoder().decode(base64);
            return new GeminiImageResult(bytes, mimeType != null ? mimeType : DEFAULT_CONTENT_TYPE);
        }

        if (first instanceof String base64) {
            byte[] bytes = Base64.getDecoder().decode(base64);
            return new GeminiImageResult(bytes, DEFAULT_CONTENT_TYPE);
        }

        throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Unsupported prediction format");
    }

    private GeminiImageResult parseGenerateContentResponse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Empty response from image provider");
        }

        Map<String, Object> response = objectMapper.readValue(body, new TypeReference<>() {});
        Object candidatesObj = response.get("candidates");
        if (!(candidatesObj instanceof List<?> candidates) || candidates.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Missing candidates in response");
        }

        for (Object candidate : candidates) {
            if (!(candidate instanceof Map<?, ?> candidateMap)) {
                continue;
            }
            GeminiImageResult result = extractImageFromCandidate(candidateMap);
            if (result != null) {
                return result;
            }
        }

        throw new BusinessException(ErrorCode.AI_PROVIDER_ERROR, "Image bytes not found in Gemini response");
    }

    private GeminiImageResult extractImageFromCandidate(Map<?, ?> candidateMap) {
        Object contentObj = candidateMap.get("content");
        if (contentObj instanceof Map<?, ?> contentMap) {
            Object partsObj = contentMap.get("parts");
            if (partsObj instanceof List<?> parts) {
                for (Object part : parts) {
                    if (part instanceof Map<?, ?> partMap) {
                        GeminiImageResult result = extractImageFromPart(partMap);
                        if (result != null) {
                            return result;
                        }
                    }
                }
            }
        }
        return null;
    }

    private GeminiImageResult extractImageFromPart(Map<?, ?> partMap) {
        Map<?, ?> inlineData = findMap(partMap, "inlineData", "inline_data");
        if (inlineData != null) {
            GeminiImageResult result = buildImageResultFromMap(inlineData);
            if (result != null) {
                return result;
            }
        }
        return buildImageResultFromMap(partMap);
    }

    private GeminiImageResult buildImageResultFromMap(Map<?, ?> map) {
        String base64 = findString(map, BASE64_KEYS);
        if (base64 == null) {
            return null;
        }
        String mimeType = findString(map, MIME_KEYS);
        byte[] bytes = Base64.getDecoder().decode(base64);
        return new GeminiImageResult(bytes, mimeType != null ? mimeType : DEFAULT_CONTENT_TYPE);
    }

    private Map<?, ?> findMap(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof Map<?, ?> nested) {
                return nested;
            }
        }
        return null;
    }

    private String findString(Map<?, ?> map, List<String> keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value instanceof String str && !str.isBlank()) {
                return str;
            }
        }
        return null;
    }

    private String resolveAccessToken() throws IOException {
        GoogleCredentials creds = getCredentials();
        AccessToken token = creds.getAccessToken();
        if (token == null || isExpiringSoon(token)) {
            token = creds.refreshAccessToken();
        }
        if (token == null || token.getTokenValue() == null) {
            throw new BusinessException(ErrorCode.AI_AUTH_FAILED, "Failed to acquire access token");
        }
        return token.getTokenValue();
    }

    private GoogleCredentials getCredentials() throws IOException {
        if (credentials == null) {
            synchronized (this) {
                if (credentials == null) {
                    credentials = GoogleCredentials.getApplicationDefault().createScoped(CLOUD_PLATFORM_SCOPE);
                }
            }
        }
        return credentials;
    }

    private boolean isExpiringSoon(AccessToken token) {
        if (token.getExpirationTime() == null) {
            return true;
        }
        Instant expiresAt = token.getExpirationTime().toInstant();
        return expiresAt.isBefore(Instant.now().plusSeconds(60));
    }

    private BusinessException mapHttpException(HttpStatusCodeException e) {
        int status = e.getRawStatusCode();
        log.warn("[GeminiImageClient] HTTP {} error: {}", status, e.getResponseBodyAsString());
        if (status == 401 || status == 403) {
            return new BusinessException(ErrorCode.AI_AUTH_FAILED, e);
        }
        if (status == 429) {
            return new BusinessException(ErrorCode.AI_RATE_LIMITED, e);
        }
        if (status == 408 || status == 504) {
            return new BusinessException(ErrorCode.AI_TIMEOUT, e);
        }
        if (status >= 500) {
            return new BusinessException(ErrorCode.AI_PROVIDER_ERROR, e);
        }
        return new BusinessException(ErrorCode.INTERNAL_ERROR, e);
    }

    private String requireNonBlank(String value, String message) {
        String trimmed = safeTrim(value);
        if (trimmed == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, message);
        }
        return trimmed;
    }

    private String safeTrim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isGeminiModel(String model) {
        return model.toLowerCase().contains("gemini");
    }
}
