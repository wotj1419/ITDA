package com.itda.backend.scenario.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scene.controller.dto.response.SceneDetailResponse;
import com.itda.backend.scene.service.SceneService;
import com.itda.backend.scene.service.dto.SceneDraft;
import com.itda.backend.scenario.controller.dto.request.GeneratePromptRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePlotRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePromptRequest;
import com.itda.backend.scenario.controller.dto.response.ScenarioPlotResponse;
import com.itda.backend.scenario.controller.dto.response.ScenarioPromptResponse;
import com.itda.backend.scenario.controller.dto.response.ScenarioResponse;
import com.itda.backend.scenario.controller.dto.response.ScenarioSceneItem;
import com.itda.backend.scenario.controller.dto.response.ScenarioScenesResponse;
import com.itda.backend.scenario.domain.ProjectScenario;
import com.itda.backend.scenario.repository.ScenarioMapper;
import com.itda.backend.scenario.repository.dto.ScenarioRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScenarioService {

    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";

    private static final String STEP_INPUT = "INPUT";
    private static final String STEP_PROMPT = "PROMPT";
    private static final String STEP_PLOT = "PLOT";
    private static final String STEP_SCENES = "SCENES";

    private static final int KEYWORDS_MAX_LENGTH = 255;

    private final ScenarioMapper scenarioMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final VertexAiGeminiClient vertexAiGeminiClient;
    private final SceneService sceneService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public ScenarioResponse getScenario(Long userId, Long projectId) {
        ensureProjectAccessible(projectId, userId);

        ProjectScenario scenario = ProjectScenario.fromRecord(getScenarioRecord(projectId));
        return ScenarioResponse.from(scenario);
    }

    @Transactional
    public ScenarioPromptResponse generatePrompt(Long userId, Long projectId, GeneratePromptRequest request) {
        ensureProjectAccessible(projectId, userId);

        String keywords = normalizeKeywords(request.keywords());
        String aiPrompt = buildPromptForPromptGeneration(request, keywords);

        String generatedPrompt = generateText(aiPrompt);
        upsertInputAndPrompt(projectId, request, keywords, generatedPrompt);

        return ScenarioPromptResponse.of(generatedPrompt, STATUS_DRAFT);
    }

    @Transactional
    public void updatePrompt(Long userId, Long projectId, UpdatePromptRequest request) {
        ensureProjectAccessible(projectId, userId);

        ScenarioRecord record = getScenarioRecord(projectId);
        ensurePromptExists(record);

        String status = request.status();
        String currentStep = resolveStep(status, STEP_PLOT, STEP_PROMPT);

        updatePromptOrThrow(projectId, request.text().trim(), status, currentStep, resolveVersion(record));
    }

    @Transactional
    public ScenarioPlotResponse generatePlot(Long userId, Long projectId) {
        ensureProjectAccessible(projectId, userId);

        ScenarioRecord record = getScenarioRecord(projectId);
        ensurePromptApproved(record);

        String aiPrompt = buildPromptForPlotGeneration(record);
        String plotText = generateText(aiPrompt);
        updatePlotOrThrow(projectId, plotText, STATUS_DRAFT, STEP_PLOT, resolveVersion(record));

        return ScenarioPlotResponse.of(plotText, STATUS_DRAFT);
    }

    @Transactional
    public void updatePlot(Long userId, Long projectId, UpdatePlotRequest request) {
        ensureProjectAccessible(projectId, userId);

        ScenarioRecord record = getScenarioRecord(projectId);
        ensurePromptApproved(record);
        ensurePlotExists(record);

        String status = request.status();
        String currentStep = resolveStep(status, STEP_SCENES, STEP_PLOT);
        updatePlotOrThrow(projectId, request.text().trim(), status, currentStep, resolveVersion(record));
    }

    @Transactional
    public ScenarioScenesResponse generateScenes(Long userId, Long projectId) {
        ensureProjectAccessible(projectId, userId);

        ScenarioRecord record = getScenarioRecord(projectId);
        ensurePromptApproved(record);
        ensurePlotApproved(record);

        String aiResponse = generateText(buildPromptForSceneGeneration(record));

        List<AiSceneItem> aiScenes = parseAiScenes(aiResponse);
        validateSceneCount(record.getInputSceneCount(), aiScenes.size());
        List<SceneDraft> drafts = toSceneDrafts(aiScenes);

        List<SceneDetailResponse> created = sceneService.createScenesAppend(userId, projectId, drafts);
        updateCurrentStepOrThrow(projectId, STEP_SCENES, resolveVersion(record));

        return ScenarioScenesResponse.of(toScenarioSceneItems(created), STEP_SCENES);
    }

    private ScenarioRecord buildScenarioRecord(Long projectId,
                                               GeneratePromptRequest request,
                                               String keywords,
                                               String promptText) {
        ScenarioRecord record = new ScenarioRecord();
        record.setProjectId(projectId);
        record.setInputGenre(request.genre());
        record.setInputMood(request.mood());
        record.setInputSceneCount(request.sceneCount());
        record.setInputKeywords(keywords);
        record.setInputCharacterHints(request.characterHints());
        record.setInputBackgroundHints(request.backgroundHints());
        record.setInputReferenceStyle(request.referenceStyle());
        record.setPromptText(promptText);
        record.setPromptStatus(STATUS_DRAFT);
        record.setPlotText(null);
        record.setPlotStatus(STATUS_DRAFT);
        record.setCurrentStep(STEP_PROMPT);
        return record;
    }

    private String normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return null;
        }
        String normalized = keywords.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.joining(", "));

        if (normalized.isEmpty()) {
            return null;
        }
        if (normalized.length() > KEYWORDS_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "keywords are too long");
        }
        return normalized;
    }

    private String buildPromptForPromptGeneration(GeneratePromptRequest request, String keywords) {
        List<String> lines = new ArrayList<>();
        lines.add("당신은 실력이 뛰어난 영화 시나리오 기획자입니다.");
        lines.add("규칙:");
        lines.add("1) 아래 입력값을 그대로 반영하되 임의로 추가/변경하지 마세요.");
        lines.add("2) 한국어로 정확히 3문장, 명령형으로 작성하세요.");
        lines.add("3) 문장 역할 고정:");
        lines.add("   - 1문장: 장르/분위기/키워드를 반영한 전체 방향 지시");
        lines.add("   - 2문장: 주인공/목표/갈등(또는 변화) 지시");
        lines.add("   - 3문장: 장면 수와 스타일/톤을 반영한 구성 지시");
        lines.add("4) 대사/목록/표 금지. 문장당 30~50자.");
        lines.add("");
        lines.add("출력 형식(문장만, 머리말/번호 없이):");
        lines.add("문장1");
        lines.add("문장2");
        lines.add("문장3");
        lines.add("");
        lines.add("입력:");
        lines.add("- 장르: " + request.genre());
        lines.add("- 분위기: " + request.mood());
        lines.add("- 장면 수: " + request.sceneCount());
        lines.add("- 키워드: " + (keywords == null ? "" : keywords));
        lines.add("- 캐릭터 힌트: " + (request.characterHints() == null ? "" : request.characterHints()));
        lines.add("- 배경 힌트: " + (request.backgroundHints() == null ? "" : request.backgroundHints()));
        lines.add("- 참고 스타일: " + (request.referenceStyle() == null ? "" : request.referenceStyle()));
        return String.join("\n", lines);
    }

    private String buildPromptForPlotGeneration(ScenarioRecord record) {
        List<String> lines = new ArrayList<>();
        lines.add("당신은 실력이 뛰어난 영화 시나리오 작가입니다.");
        lines.add("규칙:");
        lines.add("1) 한국어로 작성.");
        lines.add("2) 총 5개 문단(발단/사건/전개/클라이맥스/결말), 각 문단은 정확히 3문장.");
        lines.add("3) 프롬프트의 genre/mood/핵심 요소 변경 금지. 새로운 인물/설정/서브플롯 추가 금지.");
        lines.add("4) 대사/목록/표 금지. 서사적이고 자연스럽게 이어지는 문장으로 작성.");
        lines.add("5) 각 문단에 최소 2개 요소 포함: (주인공, 목표, 갈등/변화).");
        lines.add("6) 시간 흐름과 인과가 느껴지도록 문단·문장 간 연결성을 유지.");
        lines.add("- 장면 수: " + record.getInputSceneCount());
        lines.add("출력 형식(아래 형식 그대로):");
        lines.add("[발단]");
        lines.add("... (3문장)");
        lines.add("");
        lines.add("[사건]");
        lines.add("... (3문장)");
        lines.add("");
        lines.add("[전개]");
        lines.add("... (3문장)");
        lines.add("");
        lines.add("[클라이맥스]");
        lines.add("... (3문장)");
        lines.add("");
        lines.add("[결말]");
        lines.add("... (3문장)");
        lines.add("");
        lines.add("프롬프트:");
        lines.add("<BEGIN_PROMPT>");
        lines.add(record.getPromptText());
        lines.add("<END_PROMPT>");
        return String.join("\n", lines);
    }

    private String buildPromptForSceneGeneration(ScenarioRecord record) {
        List<String> lines = new ArrayList<>();
        lines.add("당신은 영화 시나리오 작가입니다.");
        lines.add("아래 줄거리를 바탕으로 씬을 작성하세요.");
        lines.add("- 장면 수: " + record.getInputSceneCount());
        lines.add("요구사항:");
        lines.add("1) 한국어로 작성");
        lines.add("2) 각 씬은 title(간결)과 description(1~2문장)을 포함");
        lines.add("3) 반드시 JSON 배열만 출력");
        lines.add("4) 포맷:");
        lines.add("[{\"order\":1,\"title\":\"...\",\"description\":\"...\"}, ...]");
        lines.add("줄거리:");
        lines.add(record.getPlotText());
        return String.join("\n", lines);
    }

    private String generateText(String prompt) {
        TextGenerationResponse aiResponse = vertexAiGeminiClient.generate(
                new TextGenerationRequest(prompt, null)
        );
        return aiResponse.text();
    }

    private void upsertInputAndPrompt(Long projectId,
                                      GeneratePromptRequest request,
                                      String keywords,
                                      String promptText) {
        Optional<ScenarioRecord> existing = scenarioMapper.findByProjectId(projectId);
        if (existing.isPresent()) {
            updateInputAndPrompt(projectId, request, keywords, promptText, existing.get());
            return;
        }

        ScenarioRecord insert = buildScenarioRecord(projectId, request, keywords, promptText);
        insert.setVersion(1);
        try {
            scenarioMapper.insertScenario(insert);
        } catch (DuplicateKeyException e) {
            ScenarioRecord record = getScenarioRecordOrConflict(projectId);
            updateInputAndPrompt(projectId, request, keywords, promptText, record);
        }
    }

    private void updateInputAndPrompt(Long projectId,
                                      GeneratePromptRequest request,
                                      String keywords,
                                      String promptText,
                                      ScenarioRecord existing) {
        ScenarioRecord update = buildScenarioRecord(projectId, request, keywords, promptText);
        update.setVersion(resolveVersion(existing));

        int updated = scenarioMapper.updateInputAndPrompt(update);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SCENARIO_VERSION_CONFLICT);
        }
    }

    private void updatePromptOrThrow(Long projectId,
                                     String promptText,
                                     String status,
                                     String currentStep,
                                     Integer version) {
        int updated = scenarioMapper.updatePrompt(
                projectId,
                promptText,
                status,
                currentStep,
                null,
                STATUS_DRAFT,
                version
        );

        if (updated == 0) {
            throw new BusinessException(ErrorCode.SCENARIO_VERSION_CONFLICT);
        }
    }

    private void updatePlotOrThrow(Long projectId,
                                   String plotText,
                                   String status,
                                   String currentStep,
                                   Integer version) {
        int updated = scenarioMapper.updatePlot(
                projectId,
                plotText,
                status,
                currentStep,
                version
        );

        if (updated == 0) {
            throw new BusinessException(ErrorCode.SCENARIO_VERSION_CONFLICT);
        }
    }

    private void updateCurrentStepOrThrow(Long projectId, String currentStep, Integer version) {
        int updated = scenarioMapper.updateCurrentStep(projectId, currentStep, version);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SCENARIO_VERSION_CONFLICT);
        }
    }

    private void ensureProjectAccessible(Long projectId, Long userId) {
        ensureProjectExists(projectId);
        ensureMember(projectId, userId);
    }

    private void ensureMember(Long projectId, Long userId) {
        if (!projectMemberMapper.existsMember(projectId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void ensureProjectExists(Long projectId) {
        if (projectMapper.findById(projectId).isEmpty()) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
    }

    private ScenarioRecord getScenarioRecord(Long projectId) {
        return scenarioMapper.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENARIO_NOT_FOUND));
    }

    private ScenarioRecord getScenarioRecordOrConflict(Long projectId) {
        return scenarioMapper.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENARIO_VERSION_CONFLICT));
    }

    private void ensurePromptExists(ScenarioRecord record) {
        if (record.getPromptText() == null) {
            throw new BusinessException(ErrorCode.SCENARIO_INVALID_STATE);
        }
    }

    private void ensurePromptApproved(ScenarioRecord record) {
        if (!STATUS_APPROVED.equals(record.getPromptStatus()) || record.getPromptText() == null) {
            throw new BusinessException(ErrorCode.SCENARIO_INVALID_STATE);
        }
    }

    private void ensurePlotApproved(ScenarioRecord record) {
        if (!STATUS_APPROVED.equals(record.getPlotStatus()) || record.getPlotText() == null) {
            throw new BusinessException(ErrorCode.SCENARIO_INVALID_STATE);
        }
    }

    private void ensurePlotExists(ScenarioRecord record) {
        if (record.getPlotText() == null) {
            throw new BusinessException(ErrorCode.SCENARIO_INVALID_STATE);
        }
    }

    private String resolveStep(String status, String approvedStep, String draftStep) {
        return STATUS_APPROVED.equals(status) ? approvedStep : draftStep;
    }

    private Integer resolveVersion(ScenarioRecord record) {
        return record.getVersion() != null ? record.getVersion() : 1;
    }

    private List<AiSceneItem> parseAiScenes(String aiResponse) {
        String json = extractJsonArray(aiResponse);
        try {
            List<AiSceneItem> items = objectMapper.readValue(json, new TypeReference<>() {});
            if (items == null || items.isEmpty()) {
                throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, "AI response is empty");
            }
            for (AiSceneItem item : items) {
                if (item == null || item.title() == null || item.title().isBlank()) {
                    throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, "AI response contains empty title");
                }
                if (item.description() == null || item.description().isBlank()) {
                    throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, "AI response contains empty description");
                }
            }
            return items;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, "Failed to parse AI response");
        }
    }

    private void validateSceneCount(int expectedCount, int actualCount) {
        if (actualCount != expectedCount) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID,
                    "scene count mismatch: expected " + expectedCount + " but got " + actualCount);
        }
    }

    private List<SceneDraft> toSceneDrafts(List<AiSceneItem> aiScenes) {
        return aiScenes.stream()
                .map(item -> new SceneDraft(item.title(), item.description()))
                .toList();
    }

    private List<ScenarioSceneItem> toScenarioSceneItems(List<SceneDetailResponse> scenes) {
        return scenes.stream()
                .map(scene -> new ScenarioSceneItem(
                        scene.sceneId(),
                        scene.order(),
                        scene.title(),
                        scene.description()
                ))
                .toList();
    }

    private String extractJsonArray(String text) {
        if (text == null) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, "AI response is empty");
        }
        String trimmed = text.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start == -1 || end == -1 || end <= start) {
            throw new BusinessException(ErrorCode.AI_RESPONSE_INVALID, "AI response is not JSON array");
        }
        return trimmed.substring(start, end + 1);
    }

    private record AiSceneItem(
            Integer order,
            String title,
            String description
    ) {
    }
}
