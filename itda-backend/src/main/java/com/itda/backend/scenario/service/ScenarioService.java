package com.itda.backend.scenario.service;

import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.dto.request.TextGenerationRequest;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scenario.controller.dto.request.GeneratePromptRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePlotRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePromptRequest;
import com.itda.backend.scenario.controller.dto.response.ScenarioPlotResponse;
import com.itda.backend.scenario.controller.dto.response.ScenarioPromptResponse;
import com.itda.backend.scenario.controller.dto.response.ScenarioResponse;
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
        lines.add("당신은 영화 시나리오 플래너입니다.");
        lines.add("아래 입력을 바탕으로 줄거리 생성을 위한 프롬프트를 작성하세요.");
        lines.add("- 장르: " + request.genre());
        lines.add("- 분위기: " + request.mood());
        lines.add("- 장면 수: " + request.sceneCount());
        if (keywords != null) {
            lines.add("- 키워드: " + keywords);
        }
        if (request.characterHints() != null && !request.characterHints().isBlank()) {
            lines.add("- 캐릭터 힌트: " + request.characterHints());
        }
        if (request.backgroundHints() != null && !request.backgroundHints().isBlank()) {
            lines.add("- 배경 힌트: " + request.backgroundHints());
        }
        if (request.referenceStyle() != null && !request.referenceStyle().isBlank()) {
            lines.add("- 참고 스타일: " + request.referenceStyle());
        }
        lines.add("요구사항: 한국어로 2~3문장, 간결하고 명령형 문장으로 작성하세요.");
        return String.join("\n", lines);
    }

    private String buildPromptForPlotGeneration(ScenarioRecord record) {
        List<String> lines = new ArrayList<>();
        lines.add("다음 프롬프트를 바탕으로 영화 줄거리를 작성하세요.");
        lines.add("- 장면 수: " + record.getInputSceneCount());
        lines.add("요구사항: 한국어로 4~6문장, 기승전결이 드러나도록 작성하세요.");
        lines.add("프롬프트:");
        lines.add(record.getPromptText());
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
}
