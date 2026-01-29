package com.itda.backend.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobRequestParser {

    private static final TypeReference<Map<String, Object>> SETTINGS_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<Long>> LONG_LIST_TYPE = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ParsedJobRequest parse(String requestJson) {
        if (requestJson == null || requestJson.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "request_json is empty");
        }

        try {
            JsonNode root = objectMapper.readTree(requestJson);
            if (root.isTextual()) {
                return parsePlainPrompt(root.asText());
            }
            if (root.isObject()) {
                return parseObject(root);
            }
            return parsePlainPrompt(requestJson);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse request_json as JSON. Using raw prompt. reason={}", e.getOriginalMessage());
            log.debug("request_json parse exception", e);
            return parsePlainPrompt(requestJson);
        }
    }

    private ParsedJobRequest parsePlainPrompt(String rawPrompt) {
        return new ParsedJobRequest(normalizeAndValidatePrompt(rawPrompt), null, List.of());
    }

    private ParsedJobRequest parseObject(JsonNode root) {
        String prompt = root.path("prompt").asText(null);
        Map<String, Object> settings = parseSettings(root.path("settings"));
        List<Long> referenceObjectIds = parseReferenceObjectIds(root.path("referenceObjectIds"));
        return new ParsedJobRequest(normalizeAndValidatePrompt(prompt), settings, referenceObjectIds);
    }

    private Map<String, Object> parseSettings(JsonNode settingsNode) {
        if (!settingsNode.isObject()) {
            return null;
        }
        return objectMapper.convertValue(settingsNode, SETTINGS_TYPE);
    }

    private List<Long> parseReferenceObjectIds(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "referenceObjectIds must be an array");
        }
        try {
            List<Long> ids = objectMapper.convertValue(node, LONG_LIST_TYPE);
            if (ids == null) {
                return List.of();
            }
            List<Long> normalized = new ArrayList<>();
            for (Long id : ids) {
                if (id == null || id <= 0) {
                    throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid referenceObjectIds");
                }
                normalized.add(id);
            }
            return normalized;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid referenceObjectIds: {}", e.toString());
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid referenceObjectIds");
        }
    }

    private String normalizeAndValidatePrompt(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Prompt is empty");
        }
        return prompt.trim();
    }
}
