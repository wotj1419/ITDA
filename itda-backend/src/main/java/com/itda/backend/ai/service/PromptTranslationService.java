package com.itda.backend.ai.service;

import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Prompt translation and rewrite helpers for EN <-> KO.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PromptTranslationService {

    private final VertexAiGeminiClient vertexAiGeminiClient;

    public String translateEnToKo(String promptEn) {
        String trimmed = Optional.ofNullable(promptEn).orElse("").trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String prompt = buildEnToKoPrompt(trimmed);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        return sanitizePromptLine(response == null ? null : response.text());
    }

    public String rewriteKoToEn(String promptKo) {
        String trimmed = Optional.ofNullable(promptKo).orElse("").trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String prompt = buildKoToEnPrompt(trimmed);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        return sanitizePromptLine(response == null ? null : response.text());
    }

    private String buildEnToKoPrompt(String input) {
        return String.join(
                "\n",
                "Translate the following English image/video prompt into natural Korean.",
                "Rules:",
                "- Preserve all visual details and spatial relationships.",
                "- Keep 2 to 4 sentences.",
                "- Output ONLY Korean text (no quotes, no bullet points, no JSON, no code).",
                "- Do not add information not present in the source.",
                "Prompt:",
                input
        );
    }

    private String buildKoToEnPrompt(String input) {
        return String.join(
                "\n",
                "Rewrite the following Korean description into a fluent English image/video generation prompt.",
                "Rules:",
                "- 2 to 4 sentences, descriptive paragraph (no keyword list).",
                "- Output ONLY English prompt text (no quotes, no bullets, no JSON, no code).",
                "- Preserve all concrete visual details; do not add new information.",
                "- Keep lighting physically plausible and internally consistent (single time of day, single dominant light source).",
                "Prompt:",
                input
        );
    }

    private String sanitizePromptLine(String raw) {
        String text = Optional.ofNullable(raw).orElse("").trim();
        if (text.isEmpty()) {
            return text;
        }

        text = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        text = text.replaceAll(
                "^(?:(?:[-*•]\\s*)|(?:\\d+\\s*[).]\\s*)|(?:프롬프트\\s*[:：]\\s*)|(?:prompt\\s*[:：]\\s*))+",
                ""
        ).trim();
        text = text.replaceAll("\\s{2,}", " ").trim();

        return text;
    }
}
