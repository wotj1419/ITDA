package com.itda.backend.timeline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.asset.service.AssetUrlResolver;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
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
import com.itda.backend.timeline.repository.ProjectMergeMapper;
import com.itda.backend.timeline.repository.SceneVideoMapper;
import com.itda.backend.timeline.repository.TimelineMapper;
import com.itda.backend.timeline.repository.dto.ProjectTimelineItem;
import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final SceneVideoMapper sceneVideoMapper;
    private final ProjectMergeMapper projectMergeMapper;

    @Transactional(readOnly = true)
    public SceneTimelineResponse getSceneTimeline(Long userId, Long sceneId) {
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);

        List<SceneTimelineItemResponse> items = timelineMapper.findSceneTimelineItems(sceneId).stream()
                .map(this::toSceneTimelineItemResponse)
                .toList();
        int totalDuration = sumDuration(items);

        return new SceneTimelineResponse(items, totalDuration);
    }

    @Transactional(readOnly = true)
    public ProjectTimelineResponse getProjectTimeline(Long userId, Long projectId) {
        requireProject(projectId);
        ensureMember(projectId, userId);

        List<ProjectTimelineItemResponse> items = timelineMapper.findProjectTimelineItems(projectId).stream()
                .map(this::toProjectTimelineItemResponse)
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
        String mergeSignature = mergeSignatureService.computeSceneSignature(sceneId, includeMusic, timelineItems);

        if (sceneVideoMapper.findActiveBySceneIdAndSignature(sceneId, mergeSignature).isPresent()) {
            return MergeResponse.cacheHit();
        }

        var cachedSceneVideo = sceneVideoMapper.findLatestBySceneIdAndSignature(sceneId, mergeSignature);
        if (cachedSceneVideo.isPresent()) {
            var sceneVideo = cachedSceneVideo.get();
            if ("COMPLETED".equals(sceneVideo.getStatus()) && sceneVideo.getAssetId() != null) {
                Long sceneVideoId = sceneVideo.getId();
                sceneVideoMapper.activateBySceneId(sceneId, sceneVideoId);
                timelineMapper.updateSceneTimelineItemsVideoId(sceneId, sceneVideoId);
                return MergeResponse.cacheHit();
            }
        }

        String requestJson = serializePayload(Map.of("includeMusic", includeMusic));
        Job job = jobService.createAndEnqueue(
                new JobCreateRequest(
                        JobType.SCENE_MERGE,
                        scene.getProjectId(),
                        sceneId,
                        null,
                        requestJson,
                        null,
                        mergeSignature,
                        MergeSource.SCENE),
                true);

        if (job.getStatus() == JobStatus.SUCCEEDED
                && sceneVideoMapper.findLatestBySceneIdAndSignature(sceneId, mergeSignature).isEmpty()) {
            var inProgressJob = jobService.findLatestInProgressSceneMerge(sceneId, mergeSignature);
            if (inProgressJob.isPresent()) {
                return MergeResponse.from(inProgressJob.get());
            }
            Job forcedJob = jobService.createForcedSceneMergeJob(
                    scene.getProjectId(),
                    sceneId,
                    requestJson,
                    mergeSignature);
            return MergeResponse.from(forcedJob);
        }

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
        String mergeSignature = mergeSignatureService.computeProjectSignature(projectId, includeMusic, timelineItems);

        // 캐시 체크: 동일 signature의 active 결과 존재 시 재실행 금지
        if (projectMergeMapper.findActiveByProjectIdAndSignature(projectId, mergeSignature).isPresent()) {
            return MergeResponse.cacheHit();
        }

        String requestJson = serializePayload(Map.of("includeMusic", includeMusic));
        Job job = jobService.createAndEnqueue(
                new JobCreateRequest(
                        JobType.PROJECT_MERGE,
                        projectId,
                        null,
                        null,
                        requestJson,
                        null,
                        mergeSignature,
                        MergeSource.PROJECT),
                true);

        return MergeResponse.from(job);
    }

    @Transactional
    public void reorderSceneTimeline(Long userId, Long sceneId, List<Long> orderedVideoNodeIds) {
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);
        validateOrderedIds(orderedVideoNodeIds);

        int total = timelineMapper.countSceneTimelineItems(sceneId);
        if (total == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        int matched = timelineMapper.countSceneTimelineItemsByVideoNodeIds(sceneId, orderedVideoNodeIds);
        if (matched != total || matched != orderedVideoNodeIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        timelineMapper.reorderSceneTimelineItems(sceneId, orderedVideoNodeIds);
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

    private void validateOrderedIds(List<Long> orderedIds) {
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Set<Long> uniqueIds = new HashSet<>(orderedIds);
        if (uniqueIds.size() != orderedIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private SceneTimelineItemResponse toSceneTimelineItemResponse(SceneTimelineItem item) {
        String videoUrl = assetUrlResolver.resolvePublicUrl(item.getAssetId(), item.getFallbackUrl());
        String videoThumbnail = assetUrlResolver.resolvePublicUrl(item.getThumbnailAssetId(), null);
        String shotThumbnail = assetUrlResolver.resolvePublicUrl(item.getShotAssetId(), item.getShotContentUrl());
        String masterThumbnail = assetUrlResolver.resolvePublicUrl(item.getMasterAssetId(), item.getMasterContentUrl());
        String thumbnailUrl = firstImageUrl(videoThumbnail, shotThumbnail, masterThumbnail);
        return SceneTimelineItemResponse.from(
                item,
                videoUrl,
                thumbnailUrl,
                videoUrl
        );
    }

    private ProjectTimelineItemResponse toProjectTimelineItemResponse(ProjectTimelineItem item) {
        String videoUrl = assetUrlResolver.resolvePublicUrl(item.getAssetId(), null);
        String fallbackThumbnail = assetUrlResolver.resolvePublicUrl(item.getThumbnailAssetId(), item.getFallbackUrl());
        String masterThumbnail = assetUrlResolver.resolvePublicUrl(item.getMasterAssetId(), item.getMasterContentUrl());
        String thumbnailUrl = firstImageUrl(fallbackThumbnail, masterThumbnail);
        return ProjectTimelineItemResponse.from(
                item,
                videoUrl,
                thumbnailUrl,
                videoUrl
        );
    }

    private String firstImageUrl(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String candidate : candidates) {
            if (candidate != null && !candidate.isBlank() && isImageUrl(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isImageUrl(String url) {
        if (url == null) {
            return false;
        }
        String normalized = url;
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        String lower = normalized.toLowerCase();
        return lower.endsWith(".png")
                || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".webp")
                || lower.endsWith(".gif");
    }
}
