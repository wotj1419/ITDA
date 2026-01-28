package com.itda.backend.timeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.asset.service.AssetUrlResolver;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.domain.MergeSource;
import com.itda.backend.job.service.JobCreateRequest;
import com.itda.backend.job.service.JobService;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import com.itda.backend.timeline.controller.dto.request.MergeRequest;
import com.itda.backend.timeline.controller.dto.response.MergeResponse;
import com.itda.backend.timeline.controller.dto.response.ProjectTimelineItemResponse;
import com.itda.backend.timeline.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.timeline.controller.dto.response.SceneTimelineItemResponse;
import com.itda.backend.timeline.controller.dto.response.SceneTimelineResponse;
import com.itda.backend.timeline.repository.TimelineMapper;
import com.itda.backend.timeline.repository.dto.ProjectTimelineItem;
import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final TimelineMapper timelineMapper;
    private final SceneMapper sceneMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectMapper projectMapper;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final AssetUrlResolver assetUrlResolver;
    private final MergeSignatureService mergeSignatureService;

    @Transactional(readOnly = true)
    public SceneTimelineResponse getSceneTimeline(Long userId, Long sceneId) {
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);

        List<SceneTimelineItemResponse> items = timelineMapper.findSceneTimelineItems(sceneId).stream()
                .map(item -> SceneTimelineItemResponse.from(
                        item,
                        assetUrlResolver.resolveNodeUrl(item.getAssetId(), item.getVideoNodeId(), item.getFallbackUrl())
                ))
                .toList();
        int totalDuration = sumDuration(items);

        return new SceneTimelineResponse(items, totalDuration);
    }

    @Transactional(readOnly = true)
    public ProjectTimelineResponse getProjectTimeline(Long userId, Long projectId) {
        requireProject(projectId);
        ensureMember(projectId, userId);

        List<ProjectTimelineItemResponse> items = timelineMapper.findProjectTimelineItems(projectId).stream()
                .map(item -> ProjectTimelineItemResponse.from(
                        item,
                        assetUrlResolver.resolveUrl(item.getAssetId(), item.getFallbackUrl())
                ))
                .toList();
        int totalDuration = sumProjectDuration(items);

        return new ProjectTimelineResponse(items, totalDuration);
    }

    @Transactional
    public MergeResponse requestSceneMerge(Long userId, Long sceneId, MergeRequest request) {
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);

        List<SceneTimelineItem> timelineItems = timelineMapper.findSceneTimelineItems(sceneId);
        if (timelineItems.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        boolean includeMusic = request != null && request.includeMusicOrFalse();
        String requestJson = serializePayload(Map.of("includeMusic", includeMusic));
        String mergeSignature = mergeSignatureService.sceneSignature(sceneId, includeMusic, timelineItems);

        Job job = jobService.createAndEnqueue(
                new JobCreateRequest(
                        JobType.SCENE_MERGE,
                        scene.getProjectId(),
                        sceneId,
                        null,
                        requestJson,
                        null,
                        mergeSignature,
                        MergeSource.SCENE
                ),
                true
        );

        return MergeResponse.from(job);
    }

    @Transactional
    public MergeResponse requestProjectMerge(Long userId, Long projectId, MergeRequest request) {
        requireProject(projectId);
        ensureMember(projectId, userId);

        List<ProjectTimelineItem> timelineItems = timelineMapper.findProjectTimelineItems(projectId);
        if (timelineItems.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        boolean includeMusic = request != null && request.includeMusicOrFalse();
        String requestJson = serializePayload(Map.of("includeMusic", includeMusic));
        String mergeSignature = mergeSignatureService.projectSignature(projectId, includeMusic, timelineItems);

        Job job = jobService.createAndEnqueue(
                new JobCreateRequest(
                        JobType.PROJECT_MERGE,
                        projectId,
                        null,
                        null,
                        requestJson,
                        null,
                        mergeSignature,
                        MergeSource.PROJECT
                ),
                true
        );

        return MergeResponse.from(job);
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

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
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
