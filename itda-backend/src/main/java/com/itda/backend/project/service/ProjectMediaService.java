package com.itda.backend.project.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.service.JobService;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.node.repository.dto.TimelineNodeRow;
import com.itda.backend.project.controller.dto.response.ProjectExportResponse;
import com.itda.backend.project.controller.dto.response.ProjectTimelineItem;
import com.itda.backend.project.controller.dto.response.ProjectTimelineResponse;
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

    private final ProjectAccessService projectAccessService;
    private final NodeMapper nodeMapper;
    private final JobService jobService;
    private final ObjectMapper objectMapper;
    private final FileStorageProperties fileStorageProperties;
    private final MediaUrlResolver mediaUrlResolver;

    @Transactional(readOnly = true)
    public ProjectTimelineResponse getTimeline(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        List<TimelineNodeRow> rows = nodeMapper.findConfirmedVideoNodesByProjectId(projectId);

        List<ProjectTimelineItem> items = new ArrayList<>(rows.size());
        int order = 1;
        for (TimelineNodeRow row : rows) {
            String contentUrl = mediaUrlResolver.nodeContentUrl(row.getVideoNodeId(), row.getContentUrl());
            items.add(new ProjectTimelineItem(
                    row.getVideoNodeId(),
                    row.getSceneId(),
                    order++,
                    contentUrl
            ));
        }
        return new ProjectTimelineResponse(items);
    }

    @Transactional
    public Job requestMerge(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        String requestJson = buildMergeRequestJson(projectId);
        return jobService.createAndEnqueue(
                JobType.PROJECT_MERGE,
                projectId,
                null,
                null,
                requestJson,
                null
        );
    }

    @Transactional(readOnly = true)
    public ProjectExportResponse getExport(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        Path exportPath = resolveExportPath(projectId);
        if (!Files.exists(exportPath)) {
            throw new BusinessException(ErrorCode.EXPORT_NOT_FOUND);
        }
        String exportUrl = mediaUrlResolver.projectExportUrl(projectId);
        return new ProjectExportResponse(exportUrl);
    }

    private String buildMergeRequestJson(Long projectId) {
        try {
            return objectMapper.writeValueAsString(Map.of("projectId", projectId));
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private Path resolveExportPath(Long projectId) {
        return Path.of(fileStorageProperties.getUploadDir(), "exports", String.valueOf(projectId), "final.mp4");
    }
}
