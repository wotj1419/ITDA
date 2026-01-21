package com.itda.backend.scenario.service;

import com.itda.backend.ai.VertexAiGeminiClient;
import com.itda.backend.ai.dto.response.TextGenerationResponse;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.domain.Project;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scenario.controller.dto.request.GeneratePromptRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePlotRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePromptRequest;
import com.itda.backend.scenario.repository.ScenarioMapper;
import com.itda.backend.scenario.repository.dto.ScenarioRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScenarioServiceTest {

    @Mock
    private ScenarioMapper scenarioMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private VertexAiGeminiClient vertexAiGeminiClient;

    @InjectMocks
    private ScenarioService scenarioService;

    @Test
    void generatePlot_ShouldRejectWhenPromptNotApproved() {
        Long userId = 1L;
        Long projectId = 10L;

        when(projectMapper.findById(projectId)).thenReturn(Optional.of(Project.builder().id(projectId).build()));
        when(projectMemberMapper.existsMember(projectId, userId)).thenReturn(true);

        ScenarioRecord record = new ScenarioRecord();
        record.setProjectId(projectId);
        record.setPromptStatus("DRAFT");
        record.setPromptText("prompt");
        record.setVersion(1);
        record.setInputSceneCount(5);

        when(scenarioMapper.findByProjectId(projectId)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> scenarioService.generatePlot(userId, projectId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCENARIO_INVALID_STATE);
    }

    @Test
    void generatePrompt_ShouldNormalizeKeywordsAndResetPlot() {
        Long userId = 1L;
        Long projectId = 10L;

        when(projectMapper.findById(projectId)).thenReturn(Optional.of(Project.builder().id(projectId).build()));
        when(projectMemberMapper.existsMember(projectId, userId)).thenReturn(true);
        when(vertexAiGeminiClient.generate(any())).thenReturn(new TextGenerationResponse("generated prompt"));

        ScenarioRecord existing = new ScenarioRecord();
        existing.setProjectId(projectId);
        existing.setVersion(3);

        when(scenarioMapper.findByProjectId(projectId)).thenReturn(Optional.of(existing));
        when(scenarioMapper.updateInputAndPrompt(any())).thenReturn(1);

        GeneratePromptRequest request = new GeneratePromptRequest(
                "SF",
                "HOPEFUL",
                5,
                List.of("화성", " 생존 ", "", "가족"),
                "외로운 우주인",
                "화성 기지",
                "인터스텔라"
        );

        scenarioService.generatePrompt(userId, projectId, request);

        ArgumentCaptor<ScenarioRecord> captor = ArgumentCaptor.forClass(ScenarioRecord.class);
        verify(scenarioMapper).updateInputAndPrompt(captor.capture());

        ScenarioRecord updated = captor.getValue();
        assertThat(updated.getInputKeywords()).isEqualTo("화성, 생존, 가족");
        assertThat(updated.getPlotText()).isNull();
        assertThat(updated.getPlotStatus()).isEqualTo("DRAFT");
        assertThat(updated.getPromptStatus()).isEqualTo("DRAFT");
        assertThat(updated.getCurrentStep()).isEqualTo("PROMPT");
        assertThat(updated.getVersion()).isEqualTo(3);
    }

    @Test
    void updatePrompt_ShouldResetPlot() {
        Long userId = 1L;
        Long projectId = 10L;

        when(projectMapper.findById(projectId)).thenReturn(Optional.of(Project.builder().id(projectId).build()));
        when(projectMemberMapper.existsMember(projectId, userId)).thenReturn(true);

        ScenarioRecord record = new ScenarioRecord();
        record.setProjectId(projectId);
        record.setPromptText("prompt");
        record.setPromptStatus("DRAFT");
        record.setPlotText("plot");
        record.setPlotStatus("APPROVED");
        record.setVersion(2);

        when(scenarioMapper.findByProjectId(projectId)).thenReturn(Optional.of(record));
        when(scenarioMapper.updatePrompt(anyLong(), anyString(), anyString(), anyString(), any(), anyString(), anyInt()))
                .thenReturn(1);

        UpdatePromptRequest request = new UpdatePromptRequest(" updated prompt ", "APPROVED");

        scenarioService.updatePrompt(userId, projectId, request);

        verify(scenarioMapper).updatePrompt(
                eq(projectId),
                eq("updated prompt"),
                eq("APPROVED"),
                eq("PLOT"),
                isNull(),
                eq("DRAFT"),
                eq(2)
        );
    }

    @Test
    void updatePlot_ShouldRejectWhenPromptNotApproved() {
        Long userId = 1L;
        Long projectId = 10L;

        when(projectMapper.findById(projectId)).thenReturn(Optional.of(Project.builder().id(projectId).build()));
        when(projectMemberMapper.existsMember(projectId, userId)).thenReturn(true);

        ScenarioRecord record = new ScenarioRecord();
        record.setProjectId(projectId);
        record.setPromptStatus("DRAFT");
        record.setPromptText("prompt");
        record.setPlotText("plot");
        record.setVersion(1);

        when(scenarioMapper.findByProjectId(projectId)).thenReturn(Optional.of(record));

        UpdatePlotRequest request = new UpdatePlotRequest("new plot", "APPROVED");

        assertThatThrownBy(() -> scenarioService.updatePlot(userId, projectId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SCENARIO_INVALID_STATE);
    }
}
