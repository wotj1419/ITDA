package com.itda.backend.project.service;

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
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.timeline.domain.ProjectMerge;
import com.itda.backend.timeline.repository.ProjectMergeMapper;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.node.repository.dto.TimelineNodeRow;
import com.itda.backend.project.controller.dto.response.ProjectExportResponse;
import com.itda.backend.project.controller.dto.response.ProjectTimelineItem;
import com.itda.backend.project.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.timeline.controller.dto.response.MergeResponse;
import com.itda.backend.timeline.repository.TimelineMapper;
import com.itda.backend.timeline.service.MergeSignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectMediaService {

    private static final String FILES_PREFIX = "/files/";
    private static final String MERGE_REQUEST_PROJECT_ID_KEY = "projectId";
    private static final String MERGE_REQUEST_INCLUDE_MUSIC_KEY = "includeMusic";
    private static final int TIMELINE_START_ORDER = 1;

    private final ProjectAccessService projectAccessService;
    private final NodeMapper nodeMapper;
    private final TimelineMapper timelineMapper;
    private final MergeSignatureService mergeSignatureService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final MediaUrlResolver mediaUrlResolver;
    private final ProjectMergeMapper projectMergeMapper;
    private final AssetUrlResolver assetUrlResolver;

    @Transactional(readOnly = true)
    public ProjectTimelineResponse getTimeline(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        List<TimelineNodeRow> rows = nodeMapper.findConfirmedVideoNodesByProjectId(projectId);
        return new ProjectTimelineResponse(buildTimelineItems(rows));
    }

    @Transactional
    public MergeResponse requestMerge(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        List<com.itda.backend.timeline.repository.dto.ProjectTimelineItem> timelineItems = timelineMapper
                .findProjectTimelineItems(projectId);
        if (timelineItems.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        boolean includeMusic = false;
        String mergeSignature = mergeSignatureService.computeProjectSignature(projectId, includeMusic, timelineItems);
        // 캐시 체크: 동일 signature의 active 결과 존재 확인
        if (projectMergeMapper.findActiveByProjectIdAndSignature(projectId, mergeSignature).isPresent()) {
            return MergeResponse.cacheHit();
        }
        // 캐시 미스: 새 Job 생성
        String requestJson = buildMergeRequestJson(projectId, includeMusic);
        Job job = enqueueProjectMergeJob(projectId, requestJson, mergeSignature);
        return MergeResponse.from(job);
    }

    @Transactional
    public void reorderTimeline(Long userId, Long projectId, List<Long> orderedSceneVideoIds) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        validateOrderedIds(orderedSceneVideoIds);

        int total = timelineMapper.countProjectTimelineItems(projectId);
        if (total == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        int matched = timelineMapper.countProjectTimelineItemsBySceneVideoIds(projectId, orderedSceneVideoIds);
        if (matched != total || matched != orderedSceneVideoIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        timelineMapper.reorderProjectTimelineItems(projectId, orderedSceneVideoIds);
    }

    @Transactional(readOnly = true)
    public ProjectExportResponse getExport(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        ProjectMerge activeMerge = projectMergeMapper.findActiveByProjectId(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPORT_NOT_FOUND,
                        "No active project merge result"));
        String fallbackUrl = mediaUrlResolver.projectExportUrl(projectId);
        String downloadUrl = assetUrlResolver.resolveUrl(activeMerge.getAssetId(), fallbackUrl);
        return new ProjectExportResponse(
                activeMerge.getAssetId(),
                downloadUrl,
                activeMerge.getMergeSignature(),
                activeMerge.getStatus());
    }

    private List<ProjectTimelineItem> buildTimelineItems(List<TimelineNodeRow> rows) {
        List<ProjectTimelineItem> items = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            TimelineNodeRow row = rows.get(i);
            int order = TIMELINE_START_ORDER + i;
            items.add(toTimelineItem(row, order));
        }
        return items;
    }

    private ProjectTimelineItem toTimelineItem(TimelineNodeRow row, int order) {
        String contentUrl = mediaUrlResolver.nodeContentUrl(row.getVideoNodeId(), row.getContentUrl());
        String publicUrl = resolvePublicContentUrl(row.getContentUrl());
        String videoUrl = assetUrlResolver.resolveUrl(row.getAssetId(), publicUrl);
        String thumbnailUrl = isImageUrl(publicUrl) ? publicUrl : null;
        return new ProjectTimelineItem(
                row.getVideoNodeId(),
                row.getSceneId(),
                order,
                contentUrl,
                thumbnailUrl,
                videoUrl);
    }

    private String resolvePublicContentUrl(String contentUrl) {
        if (contentUrl == null) {
            return null;
        }
        String trimmed = contentUrl.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (isAbsoluteUrl(trimmed)) {
            return trimmed;
        }
        if (trimmed.startsWith("/api/")) {
            return trimmed;
        }
        if (trimmed.startsWith(FILES_PREFIX)) {
            return trimmed;
        }
        if (trimmed.startsWith("files/")) {
            return "/" + trimmed;
        }
        if (trimmed.startsWith("/")) {
            return FILES_PREFIX + trimmed.substring(1);
        }
        return FILES_PREFIX + trimmed;
    }

    private boolean isAbsoluteUrl(String url) {
        return url.startsWith("http://") || url.startsWith("https://");
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

    private Job enqueueProjectMergeJob(Long projectId, String requestJson, String mergeSignature) {
        return jobService.createAndEnqueue(
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
    }

    private String buildMergeRequestJson(Long projectId, boolean includeMusic) {
        try {
            return objectMapper.writeValueAsString(mergeRequestPayload(projectId, includeMusic));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private Map<String, Object> mergeRequestPayload(Long projectId, boolean includeMusic) {
        return Map.of(
                MERGE_REQUEST_PROJECT_ID_KEY, projectId,
                MERGE_REQUEST_INCLUDE_MUSIC_KEY, includeMusic);
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
}
