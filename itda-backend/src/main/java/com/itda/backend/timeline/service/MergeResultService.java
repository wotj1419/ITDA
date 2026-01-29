package com.itda.backend.timeline.service;

import com.itda.backend.job.domain.Job;
import com.itda.backend.timeline.domain.ProjectMerge;
import com.itda.backend.timeline.domain.SceneVideo;
import com.itda.backend.timeline.repository.ProjectMergeMapper;
import com.itda.backend.timeline.repository.SceneVideoMapper;
import com.itda.backend.timeline.repository.TimelineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merge 결과 기록을 위한 입력 검증/정규화 서비스.
 */
@Service
@RequiredArgsConstructor
public class MergeResultService {

    private final SceneVideoMapper sceneVideoMapper;
    private final ProjectMergeMapper projectMergeMapper;
    private final TimelineMapper timelineMapper;

    public SceneMergeInput resolveSceneMergeInput(Job job,
                                                  Long resultAssetId,
                                                  Integer durationMs,
                                                  String thumbnailUrl) {
        if (job == null) {
            throw new IllegalStateException("Job is required");
        }
        Long projectId = job.getProjectId();
        if (projectId == null) {
            throw new IllegalStateException("Scene merge job missing projectId");
        }
        Long sceneId = job.getSceneId();
        if (sceneId == null) {
            throw new IllegalStateException("Scene merge job missing sceneId");
        }
        String mergeSignature = job.getMergeSignature();
        if (mergeSignature == null || mergeSignature.isBlank()) {
            throw new IllegalStateException("Scene merge job missing mergeSignature");
        }
        if (resultAssetId == null) {
            throw new IllegalStateException("Scene merge resultAssetId is required");
        }
        return new SceneMergeInput(
                projectId,
                sceneId,
                mergeSignature.trim(),
                resultAssetId,
                durationMs,
                normalizeThumbnail(thumbnailUrl)
        );
    }

    public ProjectMergeInput resolveProjectMergeInput(Job job, Long resultAssetId) {
        if (job == null) {
            throw new IllegalStateException("Job is required");
        }
        Long projectId = job.getProjectId();
        if (projectId == null) {
            throw new IllegalStateException("Project merge job missing projectId");
        }
        String mergeSignature = job.getMergeSignature();
        if (mergeSignature == null || mergeSignature.isBlank()) {
            throw new IllegalStateException("Project merge job missing mergeSignature");
        }
        if (resultAssetId == null) {
            throw new IllegalStateException("Project merge resultAssetId is required");
        }
        return new ProjectMergeInput(projectId, mergeSignature.trim(), resultAssetId);
    }

    public void deactivateActiveSceneVideo(Long sceneId) {
        if (sceneId == null) {
            throw new IllegalStateException("sceneId is required");
        }
        sceneVideoMapper.deactivateBySceneId(sceneId);
    }

    public void deactivateActiveProjectMerge(Long projectId) {
        if (projectId == null) {
            throw new IllegalStateException("projectId is required");
        }
        projectMergeMapper.deactivateByProjectId(projectId);
    }

    public Long insertSceneVideo(SceneMergeInput input) {
        if (input == null) {
            throw new IllegalStateException("SceneMergeInput is required");
        }
        SceneVideo sceneVideo = SceneVideo.builder()
                .sceneId(input.sceneId())
                .assetId(input.assetId())
                .mergeSignature(input.mergeSignature())
                .status("COMPLETED")
                .durationMs(input.durationMs())
                .thumbnailUrl(input.thumbnailUrl())
                .isActive(true)
                .build();
        sceneVideoMapper.insert(sceneVideo);
        return sceneVideo.getId();
    }

    public Long insertProjectMerge(ProjectMergeInput input) {
        if (input == null) {
            throw new IllegalStateException("ProjectMergeInput is required");
        }
        ProjectMerge projectMerge = ProjectMerge.builder()
                .projectId(input.projectId())
                .assetId(input.assetId())
                .mergeSignature(input.mergeSignature())
                .status("COMPLETED")
                .isActive(true)
                .build();
        projectMergeMapper.insert(projectMerge);
        return projectMerge.getId();
    }

    public void updateTimelineSceneVideoId(Long sceneId, Long sceneVideoId) {
        if (sceneId == null || sceneVideoId == null) {
            throw new IllegalStateException("sceneId/sceneVideoId is required");
        }
        timelineMapper.updateSceneTimelineItemsVideoId(sceneId, sceneVideoId);
    }

    @Transactional
    public Long recordSceneMergeResult(Job job,
                                       Long resultAssetId,
                                       Integer durationMs,
                                       String thumbnailUrl) {
        SceneMergeInput input = resolveSceneMergeInput(job, resultAssetId, durationMs, thumbnailUrl);
        deactivateActiveSceneVideo(input.sceneId());
        Long sceneVideoId = insertSceneVideo(input);
        updateTimelineSceneVideoId(input.sceneId(), sceneVideoId);
        return sceneVideoId;
    }

    public Long recordSceneMergeResult(Job job, Long resultAssetId) {
        return recordSceneMergeResult(job, resultAssetId, null, null);
    }

    @Transactional
    public Long recordProjectMergeResult(Job job, Long resultAssetId) {
        ProjectMergeInput input = resolveProjectMergeInput(job, resultAssetId);
        deactivateActiveProjectMerge(input.projectId());
        return insertProjectMerge(input);
    }

    private String normalizeThumbnail(String thumbnailUrl) {
        if (thumbnailUrl == null) {
            return null;
        }
        String trimmed = thumbnailUrl.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record SceneMergeInput(Long projectId,
                                  Long sceneId,
                                  String mergeSignature,
                                  Long assetId,
                                  Integer durationMs,
                                  String thumbnailUrl) {
    }

    public record ProjectMergeInput(Long projectId,
                                    String mergeSignature,
                                    Long assetId) {
    }
}
