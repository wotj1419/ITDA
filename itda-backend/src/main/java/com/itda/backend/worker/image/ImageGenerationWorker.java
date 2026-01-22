package com.itda.backend.worker.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.ai.gemini.GeminiImageClient;
import com.itda.backend.ai.gemini.GeminiImageResult;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenerationWorker {

    private final GeminiImageClient geminiImageClient;
    private final ImageStorage imageStorage;
    private final AssetMapper assetMapper;
    private final ObjectMapper objectMapper;

    public Long execute(Job job) {
        if (job == null) {
            throw new IllegalArgumentException("job must not be null");
        }

        String prompt = extractPrompt(job.getRequestJson());
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is required");
        }

        GeminiImageResult imageResult = geminiImageClient.generateImage(prompt);
        ImageStorageResult stored = imageStorage.save(
                job.getProjectId(),
                job.getId(),
                imageResult.bytes(),
                imageResult.contentType()
        );

        Asset asset = Asset.builder()
                .ownerId(null)
                .projectId(job.getProjectId())
                .assetType(AssetType.IMAGE)
                .storageProvider(stored.storageProvider())
                .storageKey(stored.storageKey())
                .contentType(stored.contentType())
                .sizeBytes(stored.sizeBytes())
                .build();

        assetMapper.insert(asset);
        return asset.getId();
    }

    private String extractPrompt(String requestJson) {
        if (requestJson == null) {
            return null;
        }
        String trimmed = requestJson.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            JsonNode node = objectMapper.readTree(trimmed);
            if (node.isObject() && node.hasNonNull("prompt")) {
                return node.get("prompt").asText();
            }
            if (node.isTextual()) {
                return node.asText();
            }
        } catch (Exception e) {
            log.warn("[ImageGenerationWorker] Failed to parse request_json. Using raw text.");
            return trimmed;
        }

        return trimmed;
    }
}
