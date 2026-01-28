package com.itda.backend.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.domain.MergeSource;
import com.itda.backend.job.service.JobCreateRequest;
import com.itda.backend.job.service.JobService;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.node.repository.dto.TimelineNodeRow;
import com.itda.backend.project.controller.dto.response.ProjectExportResponse;
import com.itda.backend.project.controller.dto.response.ProjectTimelineItem;
import com.itda.backend.project.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.timeline.repository.TimelineMapper;
import com.itda.backend.timeline.service.MergeSignatureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectMediaService {

    private static final String MERGE_REQUEST_PROJECT_ID_KEY = "projectId";
    private static final String MERGE_REQUEST_INCLUDE_MUSIC_KEY = "includeMusic";
    private static final String EXPORTS_DIR = "exports";
    private static final String EXPORT_FILE_NAME = "final.mp4";
    private static final int TIMELINE_START_ORDER = 1;

    private final ProjectAccessService projectAccessService;
    private final NodeMapper nodeMapper;
    private final TimelineMapper timelineMapper;
    private final MergeSignatureService mergeSignatureService;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final FileStorageProperties fileStorageProperties;
    private final MediaUrlResolver mediaUrlResolver;

    @Transactional(readOnly = true)
    public ProjectTimelineResponse getTimeline(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        List<TimelineNodeRow> rows = nodeMapper.findConfirmedVideoNodesByProjectId(projectId);
        return new ProjectTimelineResponse(buildTimelineItems(rows));
    }

    @Transactional
    public Job requestMerge(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        List<com.itda.backend.timeline.repository.dto.ProjectTimelineItem> timelineItems =
                timelineMapper.findProjectTimelineItems(projectId);
        if (timelineItems.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        boolean includeMusic = false;
        String requestJson = buildMergeRequestJson(projectId, includeMusic);
        String mergeSignature = mergeSignatureService.projectSignature(projectId, includeMusic, timelineItems);
        return enqueueProjectMergeJob(projectId, requestJson, mergeSignature);
    }

    @Transactional(readOnly = true)
    public ProjectExportResponse getExport(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        Path exportPath = resolveExportPath(projectId);
        ensureExportExists(exportPath);
        String exportUrl = mediaUrlResolver.projectExportUrl(projectId);
        return new ProjectExportResponse(exportUrl);
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
        return new ProjectTimelineItem(
                row.getVideoNodeId(),
                row.getSceneId(),
                order,
                contentUrl
        );
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
                        MergeSource.PROJECT
                ),
                true
        );
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
                MERGE_REQUEST_INCLUDE_MUSIC_KEY, includeMusic
        );
    }

    private void ensureExportExists(Path exportPath) {
        if (!Files.exists(exportPath)) {
            throw new BusinessException(ErrorCode.EXPORT_NOT_FOUND);
        }
    }

    private Path resolveExportPath(Long projectId) {
        return Path.of(
                fileStorageProperties.getUploadDir(),
                EXPORTS_DIR,
                String.valueOf(projectId),
                EXPORT_FILE_NAME
        );
    }
}
