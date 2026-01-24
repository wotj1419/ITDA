package com.itda.backend.ai.service;

import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.controller.dto.request.AiPromptGenerateRequest;
import com.itda.backend.ai.controller.dto.request.AiPromptImproveRequest;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.node.domain.NodeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 프롬프트 생성 서비스
 */
@Service
@RequiredArgsConstructor
public class AiPromptService {

    private final VertexAiGeminiClient vertexAiGeminiClient;

    public String generatePrompt(AiPromptGenerateRequest request) {
        String prompt = buildGeneratePrompt(request);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        return response.text();
    }

    public String improvePrompt(AiPromptImproveRequest request) {
        String prompt = buildImprovePrompt(request);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        return response.text();
    }

    private String buildGeneratePrompt(AiPromptGenerateRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("다음 정보를 바탕으로 한국어 프롬프트를 1~2문장으로 작성하세요.");
        lines.add("반드시 아래 입력 값을 그대로 포함하세요.");
        lines.add("- 노드 타입: " + safe(request.nodeType()));
        lines.add("- 장면 한줄: " + safe(request.sceneOneLine()));
        lines.add("- 스타일: " + safe(request.style()));
        lines.add("- 시간대: " + safe(request.timeOfDay()));
        lines.add("- 분위기: " + safe(request.mood()));
        if (request.objects() != null && !request.objects().isEmpty()) {
            lines.add("- 오브젝트: " + String.join(", ", request.objects()));
        } else {
            lines.add("- 오브젝트: 없음");
        }
        lines.add("출력은 프롬프트 문장만 제공하고, 불릿/JSON/코드는 금지.");
        return String.join("\n", lines);
    }

    private String buildImprovePrompt(AiPromptImproveRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("다음 프롬프트를 개선하세요.");
        lines.add("반드시 기존 프롬프트의 핵심 키워드를 유지하고 한국어로 1~2문장으로 출력하세요.");
        lines.add("- 노드 타입: " + safe(request.nodeType()));
        if (request.instruction() != null && !request.instruction().isBlank()) {
            lines.add("- 개선 지시: " + request.instruction().trim());
        }
        lines.add("프롬프트:");
        lines.add(safe(request.prompt()));
        lines.add("출력은 프롬프트 문장만 제공하고, 불릿/JSON/코드는 금지.");
        return String.join("\n", lines);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "없음" : value.trim();
    }

    private String safe(NodeType nodeType) {
        return nodeType == null ? "없음" : nodeType.name();
    }
}
