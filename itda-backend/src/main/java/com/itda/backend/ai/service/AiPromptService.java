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
import java.util.Optional;

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
        return sanitizePromptKo(response.text());
    }

    public String improvePrompt(AiPromptImproveRequest request) {
        String prompt = buildImprovePrompt(request);
        TextGenerationResponse response = vertexAiGeminiClient.generate(new TextGenerationRequest(prompt, null));
        return sanitizePromptKo(response.text());
    }

    private String buildGeneratePrompt(AiPromptGenerateRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("다음 정보를 바탕으로 한국어 프롬프트를 1~2문장으로 작성하세요.");
        lines.add("출력 형식: 프롬프트 문장만. 줄바꿈/불릿/번호/접두어(\"프롬프트:\")/JSON/코드 금지.");
        lines.add("톤(분위기)은 감정/서사(예: 슬픔/외로움) 대신 조명/색감/콘트라스트로만 표현하세요.");

        NodeType nodeType = request == null ? null : request.nodeType();
        if (nodeType != null) {
            lines.add(switch (nodeType) {
                case MASTER -> "가이드: 씬의 기준 룩을 잡는 와이드 establishing shot을 떠올리게 쓰세요.";
                case GRID -> "가이드: 한 장의 스토리보드 그리드 이미지(같은 순간/같은 장면, 프레이밍만 변화)를 떠올리게 쓰세요.";
                case SHOT -> "가이드: 선택된 컷을 고품질 단일 프레임으로 재생성하는 느낌으로 쓰세요.";
                case VIDEO -> "가이드: 단일 연속 숏(컷/시간점프 없음)으로 자연스러운 움직임을 유도하세요.";
                case SCENE_HEADER -> "가이드: 장면의 제목/설명 컨텍스트를 요약하는 느낌으로 쓰세요.";
            });
        }

        lines.add("노드 타입: " + safe(nodeType));
        lines.add("장면 한줄: " + safe(request == null ? null : request.sceneOneLine()));
        lines.add("스타일: " + safe(request == null ? null : request.style()));
        lines.add("시간대: " + safe(request == null ? null : request.timeOfDay()));
        lines.add("톤(조명/색감): " + safe(request == null ? null : request.mood()));
        if (request != null && request.objects() != null && !request.objects().isEmpty()) {
            lines.add("오브젝트: " + String.join(", ", request.objects()));
        } else {
            lines.add("오브젝트: 없음");
        }
        return String.join("\n", lines);
    }

    private String buildImprovePrompt(AiPromptImproveRequest request) {
        List<String> lines = new ArrayList<>();
        lines.add("다음 프롬프트를 개선하세요.");
        lines.add("기존 프롬프트의 핵심 키워드를 유지하고 한국어로 1~2문장으로 출력하세요.");
        lines.add("출력 형식: 프롬프트 문장만. 줄바꿈/불릿/번호/접두어(\"프롬프트:\")/JSON/코드 금지.");
        lines.add("톤(분위기)은 감정/서사 대신 조명/색감/콘트라스트로만 표현하세요.");
        lines.add("노드 타입: " + safe(request.nodeType()));
        if (request.instruction() != null && !request.instruction().isBlank()) {
            lines.add("개선 지시: " + request.instruction().trim());
        }
        lines.add("대상 프롬프트: " + safe(request.prompt()));
        return String.join("\n", lines);
    }

    private String sanitizePromptKo(String raw) {
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

    private String safe(String value) {
        return value == null || value.isBlank() ? "없음" : value.trim();
    }

    private String safe(NodeType nodeType) {
        return nodeType == null ? "없음" : nodeType.name();
    }
}
