package com.itda.backend.ai.veo;

import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class VeoPromptSanitizer {

    private final VertexAiGeminiClient vertexAiGeminiClient;
    private final VeoPromptSafetyValidator validator;

    public String sanitizeIfNeeded(String prompt) {
        String trimmed = Optional.ofNullable(prompt).orElse("").trim();
        if (trimmed.isEmpty()) {
            return trimmed;
        }

        List<String> violations = validator.findViolations(trimmed);
        if (violations.isEmpty()) {
            return trimmed;
        }

        String rewritten = rewritePrompt(trimmed, violations);
        if (rewritten.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Video prompt could not be sanitized. Please rephrase without real people or sensitive content."
            );
        }

        List<String> remaining = validator.findViolations(rewritten);
        if (!remaining.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Video prompt could not be sanitized: " + String.join(", ", remaining)
                            + ". Please rephrase without real people or sensitive content."
            );
        }

        return rewritten;
    }

    private String rewritePrompt(String prompt, List<String> violations) {
        try {
            String instruction = buildRewritePrompt(prompt, violations);
            TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(instruction, null));
            return sanitizePromptLine(response == null ? null : response.text());
        } catch (Exception e) {
            log.warn("Failed to sanitize video prompt: reason={}", e.getMessage());
            return "";
        }
    }

    private String buildRewritePrompt(String prompt, List<String> violations) {
        return String.join(
                "\n",
                "Rewrite the following English video prompt to comply with Vertex AI usage guidelines.",
                "Rules:",
                "- Remove any references to real people, celebrities, public figures, or minors.",
                "- Remove sexual content, graphic violence, hate/harassment, or self-harm.",
                "- Keep the core scene intent and visual details, but make all characters fictional.",
                "- 2 to 4 sentences, descriptive paragraph (no keyword list).",
                "- Output ONLY the rewritten English prompt (no quotes, no bullets, no JSON, no code).",
                "Violations detected: " + String.join(", ", violations),
                "Prompt:",
                prompt
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
