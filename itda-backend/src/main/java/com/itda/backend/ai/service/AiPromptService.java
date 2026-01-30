package com.itda.backend.ai.service;

import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.controller.dto.request.AiPromptGenerateRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptImproveRequest;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.ai.controller.dto.response.AiPromptResponse;
import com.itda.backend.node.domain.NodeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * AI 프롬프트 생성 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiPromptService {

    private final VertexAiGeminiClient vertexAiGeminiClient;
    private final PromptTranslationService promptTranslationService;

    public AiPromptResponse generatePrompt(AiPromptGenerateRequest request) {
        String prompt = buildGeneratePrompt(request);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        String promptEnBase = sanitizePromptLine(response == null ? null : response.text());
        String promptKo = "";
        try {
            promptKo = promptTranslationService.translateEnToKo(promptEnBase);
        } catch (Exception e) {
            log.warn("Failed to translate prompt to Korean: reason={}", e.getMessage());
        }
        logGeneratedPrompt(request, promptEnBase, promptKo);
        return new AiPromptResponse(promptEnBase, promptKo);
    }

    public AiPromptResponse improvePrompt(AiPromptImproveRequest request) {
        String prompt = buildImprovePrompt(request);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        String promptEnBase = sanitizePromptLine(response == null ? null : response.text());
        String promptKo = "";
        try {
            promptKo = promptTranslationService.translateEnToKo(promptEnBase);
        } catch (Exception e) {
            log.warn("Failed to translate prompt to Korean: reason={}", e.getMessage());
        }
        return new AiPromptResponse(promptEnBase, promptKo);
    }

    private String buildGeneratePrompt(AiPromptGenerateRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("Write an English prompt for image/video generation.");
        lines.add("- Describe ONE coherent scene as 2 to 4 sentences (no keyword list).");
        lines.add("- Output ONLY the prompt text (no bullets, no numbering, no quotes, no JSON, no code, no 'prompt:' prefix).");
        lines.add("- Focus on subject appearance, action, environment, and camera framing/angle.");
        lines.add("- Keep lighting physically plausible: single time of day, single dominant light source; avoid contradictory color/lighting instructions.");
        lines.add("- Do not mention watermarks, subtitles, captions, or logos.");
        lines.add("");

        NodeType nodeType = request == null ? null : request.nodeType();
        if (nodeType != null) {
            lines.add(switch (nodeType) {
                case MASTER -> "Guide: wide establishing shot; include environment, layout, and key props.";
                case GRID -> "Guide: describe the shared scene moment; avoid sequencing; keep details consistent across panels.";
                case SHOT -> "Guide: focus on a single frame with clear subject pose, gaze, hands, and foreground/background relation.";
                case VIDEO -> "Guide: single continuous shot; describe a natural motion arc from start to end (no cuts).";
                case SCENE_HEADER -> "Guide: summarize the scene context briefly.";
            });
            lines.add("");
        }

        lines.add("Inputs:");
        lines.add("nodeType: " + safe(nodeType));
        lines.add("sceneOneLine: " + safe(request == null ? null : request.sceneOneLine()));
        lines.add("style(optional constraint): " + safe(request == null ? null : request.style()));
        lines.add("timeOfDay(optional constraint): " + safe(request == null ? null : request.timeOfDay()));
        lines.add("mood(optional constraint): " + safe(request == null ? null : request.mood()));
        if (request.objects() != null && !request.objects().isEmpty()) {
            lines.add("Objects: " + String.join(", ", request.objects()));
        } else {
            lines.add("Objects: none");
        }
        return String.join("\n", lines);
    }

    private String buildImprovePrompt(AiPromptImproveRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("Improve the following English image/video prompt.");
        lines.add("- Keep the core content, but make it more concrete and visually specific.");
        lines.add("- Output 2 to 4 sentences, English only.");
        lines.add("- Output ONLY the prompt text (no bullets, no numbering, no JSON, no code).");
        lines.add("- Keep lighting physically plausible and internally consistent.");
        lines.add("");
        lines.add("nodeType: " + safe(request.nodeType()));
        if (request.instruction() != null && !request.instruction().isBlank()) {
            lines.add("userFeedback: " + request.instruction().trim());
        }
        lines.add("promptEnBase: " + safe(request.prompt()));
        return String.join("\n", lines);
    }

    private String sanitizePromptLine(String raw) {
        String text = Optional.ofNullable(raw).orElse("").trim();
        if (text.isEmpty()) {
            return text;
        }

        // Collapse multi-line outputs into a single line and strip common "prompt:" style prefixes / bullets.
        text = text.replaceAll("[\\r\\n\\t]+", " ").trim();
        text = text.replaceAll(
                "^(?:(?:[-*•]\\s*)|(?:\\d+\\s*[).]\\s*)|(?:프롬프트\\s*[:：]\\s*)|(?:prompt\\s*[:：]\\s*))+",
                ""
        ).trim();
        text = text.replaceAll("\\s{2,}", " ").trim();

        return text;
    }

    private void logGeneratedPrompt(AiPromptGenerateRequest request, String promptEnBase, String promptKo) {
        log.info(
                "Prompt generate: nodeType={}, promptEnBase={}, promptKo={}",
                request == null ? null : request.nodeType(),
                promptEnBase,
                promptKo
        );
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "none" : value.trim();
    }

    private String safe(NodeType nodeType) {
        return nodeType == null ? "none" : nodeType.name();
    }
}
