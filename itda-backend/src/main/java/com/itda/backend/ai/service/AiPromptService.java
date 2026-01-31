package com.itda.backend.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.controller.dto.request.AiPromptGenerateRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptImproveRequest;
import com.itda.backend.ai.controller.dto.response.AiPromptResponse;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
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

    private static final int DEFAULT_TIMELINE_INTERVAL_SECONDS = 2;

    private final VertexAiGeminiClient vertexAiGeminiClient;
    private final PromptTranslationService promptTranslationService;
    private final ObjectMapper objectMapper;

    public AiPromptResponse generatePrompt(AiPromptGenerateRequest request) {
        String promptEnBase = resolvePromptEnBase(request);
        String promptKo = "";
        try {
            promptKo = promptTranslationService.translateEnToKo(promptEnBase);
        } catch (Exception e) {
            log.warn("Failed to translate prompt to Korean: reason={}", e.getMessage());
        }
        var timelineCuts = generateTimelineCutsIfNeeded(request, promptEnBase);
        logGeneratedPrompt(request, promptEnBase, promptKo);
        return new AiPromptResponse(promptEnBase, promptKo, timelineCuts);
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
        return new AiPromptResponse(promptEnBase, promptKo, null);
    }

    private String resolvePromptEnBase(AiPromptGenerateRequest request) {
        String provided = sanitizePromptLine(request == null ? null : request.prompt());
        if (!provided.isBlank()) {
            return provided;
        }
        String prompt = buildGeneratePrompt(request);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        return sanitizePromptLine(response == null ? null : response.text());
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
                case MASTER -> "Guide: opening establishing still frame; wide shot; include environment, layout, key props; no temporal progression or passing-by.";
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

    private List<String> generateTimelineCutsIfNeeded(AiPromptGenerateRequest request, String promptEnBase) {
        if (request == null || request.nodeType() != NodeType.GRID) {
            return null;
        }
        String gridMode = safe(request.gridMode());
        if (!gridMode.equalsIgnoreCase("STORY_BEATS")) {
            return null;
        }
        String layout = safe(request.layout());
        int panelCount = resolvePanelCount(layout);
        if (panelCount <= 0 || promptEnBase.isBlank()) {
            return null;
        }
        int intervalSeconds = request.timelineIntervalSeconds() == null
                ? DEFAULT_TIMELINE_INTERVAL_SECONDS
                : Math.max(1, request.timelineIntervalSeconds());
        String prompt = buildTimelineCutsPrompt(promptEnBase, panelCount, intervalSeconds);
        try {
            TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
            List<String> parsed = parseTimelineCuts(response == null ? null : response.text());
            return normalizeTimelineCuts(parsed, panelCount);
        } catch (Exception e) {
            log.warn("Failed to generate timeline cuts: reason={}", e.getMessage());
            return normalizeTimelineCuts(List.of(), panelCount);
        }
    }

    private String buildTimelineCutsPrompt(String promptEnBase, int panelCount, int intervalSeconds) {
        return String.join(
                "\n",
                "You are given an English scene description for a storyboard grid.",
                "Create " + panelCount + " Korean descriptions of sequential still frames sampled every " + intervalSeconds + " seconds.",
                "Rules:",
                "- Each item describes a single frozen moment (no transitions, no motion blur).",
                "- Keep characters, outfits, lighting, and location consistent across all panels.",
                "- Do not add new elements not present in the source.",
                "- Each item should be 1-2 short sentences in Korean.",
                "- Output ONLY a JSON array of " + panelCount + " strings (no extra text).",
                "Source prompt:",
                promptEnBase
        );
    }

    private List<String> parseTimelineCuts(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            String json = trimmed.substring(start, end + 1);
            try {
                List<String> parsed = objectMapper.readValue(json, new TypeReference<List<String>>() {});
                return parsed == null ? List.of() : parsed;
            } catch (Exception ignored) {
                // fall through to line parsing
            }
        }
        String[] lines = trimmed.split("\\r?\\n");
        List<String> result = new ArrayList<>();
        for (String line : lines) {
            String normalized = sanitizePromptLine(line);
            if (!normalized.isBlank()) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<String> normalizeTimelineCuts(List<String> cuts, int panelCount) {
        List<String> normalized = new ArrayList<>();
        if (cuts != null) {
            for (String cut : cuts) {
                String cleaned = sanitizePromptLine(cut);
                if (!cleaned.isBlank()) {
                    normalized.add(cleaned);
                }
            }
        }
        while (normalized.size() < panelCount) {
            normalized.add("");
        }
        if (normalized.size() > panelCount) {
            return new ArrayList<>(normalized.subList(0, panelCount));
        }
        return normalized;
    }

    private int resolvePanelCount(String layout) {
        if (layout == null || layout.isBlank()) {
            return 0;
        }
        String[] parts = layout.trim().toLowerCase().split("x");
        if (parts.length != 2) {
            return 0;
        }
        try {
            int rows = Integer.parseInt(parts[0]);
            int cols = Integer.parseInt(parts[1]);
            return Math.max(0, rows * cols);
        } catch (NumberFormatException e) {
            return 0;
        }
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
