package com.itda.backend.timeline.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import com.itda.backend.timeline.controller.dto.response.ProjectTimelineItemResponse;
import com.itda.backend.timeline.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.timeline.controller.dto.response.SceneTimelineItemResponse;
import com.itda.backend.timeline.controller.dto.response.SceneTimelineResponse;
import com.itda.backend.timeline.repository.TimelineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineMapper timelineMapper;
    private final SceneMapper sceneMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectMapper projectMapper;

    @Transactional(readOnly = true)
    public SceneTimelineResponse getSceneTimeline(Long userId, Long sceneId) {
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);

        List<SceneTimelineItemResponse> items = timelineMapper.findSceneTimelineItems(sceneId).stream()
                .map(SceneTimelineItemResponse::from)
                .toList();
        int totalDuration = sumDuration(items);

        return new SceneTimelineResponse(items, totalDuration);
    }

    @Transactional(readOnly = true)
    public ProjectTimelineResponse getProjectTimeline(Long userId, Long projectId) {
        requireProject(projectId);
        ensureMember(projectId, userId);

        List<ProjectTimelineItemResponse> items = timelineMapper.findProjectTimelineItems(projectId).stream()
                .map(ProjectTimelineItemResponse::from)
                .toList();
        int totalDuration = sumProjectDuration(items);

        return new ProjectTimelineResponse(items, totalDuration);
    }

    private int sumDuration(List<SceneTimelineItemResponse> items) {
        return items.stream()
                .map(SceneTimelineItemResponse::duration)
                .mapToInt(value -> value != null ? value : 0)
                .sum();
    }

    private int sumProjectDuration(List<ProjectTimelineItemResponse> items) {
        return items.stream()
                .map(ProjectTimelineItemResponse::duration)
                .mapToInt(value -> value != null ? value : 0)
                .sum();
    }

    private void requireProject(Long projectId) {
        projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Scene requireScene(Long sceneId) {
        return sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
    }

    private void ensureMember(Long projectId, Long userId) {
        if (!projectMemberMapper.existsMember(projectId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
