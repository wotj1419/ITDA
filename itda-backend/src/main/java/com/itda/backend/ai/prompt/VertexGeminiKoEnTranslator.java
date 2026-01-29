package com.itda.backend.ai.prompt;

import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class VertexGeminiKoEnTranslator implements KoEnTranslator {

    private final VertexAiGeminiClient vertexAiGeminiClient;
    private final ObjectMapper objectMapper;

    @Override
    public String toEnglishOneSentence(String text) {
        String trimmed = Optional.ofNullable(text).map(String::trim).orElse("");
        if (trimmed.isEmpty()) {
            return "";
        }

        return translateSingle(trimmed);
    }

    private String buildPrompt(String input) {
        return String.join(
                "\n",
                "Translate and summarize the following text into exactly ONE natural English sentence.",
                "- Output ONLY the English sentence (no quotes, no bullet points, no JSON).",
                "- If the input contains mixed languages, normalize the output to English only.",
                "- Keep key proper nouns and concrete details.",
                "Text:",
                input
        );
    }

    @Override
    public Map<String, String> toEnglishOneSentenceMany(Map<String, String> texts) {
        if (texts == null || texts.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalizedInput = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : texts.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isBlank()) {
                continue;
            }
            String value = Optional.ofNullable(entry.getValue()).map(String::trim).orElse("");
            normalizedInput.put(key, value);
        }
        if (normalizedInput.isEmpty()) {
            return Map.of();
        }

        try {
            String prompt = buildBatchPrompt(normalizedInput);
            TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
            Map<String, String> parsed = parseJsonMap(response == null ? null : response.text());
            if (parsed != null && !parsed.isEmpty()) {
                return fillMissingWithEmpty(normalizedInput, parsed);
            }
        } catch (Exception ignored) {
            // fall through to per-item translation
        }

        Map<String, String> fallback = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : normalizedInput.entrySet()) {
            String value = entry.getValue();
            fallback.put(entry.getKey(), value.isEmpty() ? "" : translateSingle(value));
        }
        return fallback;
    }

    private String buildBatchPrompt(Map<String, String> inputs) throws Exception {
        String json = objectMapper.writeValueAsString(inputs);
        return String.join(
                "\n",
                "You are given a JSON object of key-value pairs.",
                "For each key, translate and summarize the value into exactly ONE natural English sentence.",
                "Rules:",
                "- Output ONLY a valid JSON object with the SAME keys.",
                "- Each value must be a single English sentence string (no bullet points, no markdown, no extra keys).",
                "- If the input value is blank, output an empty string for that key.",
                "- If the input contains mixed languages, normalize the output to English only.",
                "Input JSON:",
                json
        );
    }

    private Map<String, String> parseJsonMap(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            return null;
        }
        String json = trimmed.substring(start, end + 1);
        return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
    }

    private Map<String, String> fillMissingWithEmpty(Map<String, String> requestedKeys, Map<String, String> parsed) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : requestedKeys.keySet()) {
            String value = parsed.get(key);
            result.put(key, normalizeOneSentence(value));
        }
        return result;
    }

    private String translateSingle(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String prompt = buildPrompt(input);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        return normalizeOneSentence(response == null ? null : response.text());
    }

    private String normalizeOneSentence(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        int newline = trimmed.indexOf('\n');
        String firstLine = newline >= 0 ? trimmed.substring(0, newline).trim() : trimmed;
        if (firstLine.startsWith("\"") && firstLine.endsWith("\"") && firstLine.length() >= 2) {
            firstLine = firstLine.substring(1, firstLine.length() - 1).trim();
        }
        return firstLine;
    }
}
