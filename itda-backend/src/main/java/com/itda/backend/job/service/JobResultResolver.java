package com.itda.backend.job.service;

import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.node.domain.Node;
import com.itda.backend.node.repository.NodeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Job 결과 URL 해석기
 * <p>
 * Job 결과(Asset ID)를 presigned URL로 변환.
 * 현재는 스텁 구현이며, AssetService 연동 시 실제 구현 필요.
 */
@Component
@RequiredArgsConstructor
public class JobResultResolver {

    // TODO: AssetService 주입 후 presigned URL 생성 로직 구현
    // private final AssetService assetService;

    private final NodeMapper nodeMapper;
    private final FileStorageProperties fileStorageProperties;
    private final MediaUrlResolver mediaUrlResolver;

    /**
     * Job 결과를 presigned URL로 변환
     *
     * @param job 대상 Job
     * @return presigned URL (결과가 없으면 null)
     */
    public String resolve(Job job) {
        if (job == null || job.getStatus() != JobStatus.SUCCEEDED) {
            return null;
        }

        if (job.getType() == JobType.IMAGE_GENERATION || job.getType() == JobType.VIDEO_GENERATION) {
            return resolveNodeContentUrl(job);
        }
        if (job.getType() == JobType.PROJECT_MERGE) {
            return resolveExportUrl(job.getProjectId());
        }
        return null;
    }

    private String resolveNodeContentUrl(Job job) {
        if (job.getNodeId() == null) {
            return null;
        }
        Node node = nodeMapper.findById(job.getNodeId()).orElse(null);
        if (node == null || node.getContentUrl() == null) {
            return null;
        }
        return mediaUrlResolver.nodeContentUrl(node);
    }

    private String resolveExportUrl(Long projectId) {
        if (projectId == null) {
            return null;
        }
        Path exportPath = Path.of(fileStorageProperties.getUploadDir(), "exports", String.valueOf(projectId), "final.mp4");
        if (!Files.exists(exportPath)) {
            return null;
        }
        return mediaUrlResolver.projectExportUrl(projectId);
    }
}
