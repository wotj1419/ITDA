package com.itda.backend.job.service;

import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.node.repository.NodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class JobResultResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolve_ShouldReturnSceneExportUrl_WhenSceneMergeSucceededAndFileExists() throws IOException {
        long sceneId = 55L;
        Path exportPath = tempDir.resolve("exports/scenes/" + sceneId + "/final.mp4");
        Files.createDirectories(exportPath.getParent());
        Files.write(exportPath, "dummy".getBytes());

        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(tempDir.toString());

        JobResultResolver resolver = new JobResultResolver(mock(NodeMapper.class), props, new MediaUrlResolver());
        Job job = Job.builder()
                .id(1L)
                .type(JobType.SCENE_MERGE)
                .status(JobStatus.SUCCEEDED)
                .sceneId(sceneId)
                .build();

        assertThat(resolver.resolve(job)).isEqualTo("/api/scenes/55/export/file");
    }

    @Test
    void resolve_ShouldReturnNull_WhenSceneMergeSucceededButFileMissing() {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(tempDir.toString());

        JobResultResolver resolver = new JobResultResolver(mock(NodeMapper.class), props, new MediaUrlResolver());
        Job job = Job.builder()
                .id(1L)
                .type(JobType.SCENE_MERGE)
                .status(JobStatus.SUCCEEDED)
                .sceneId(55L)
                .build();

        assertThat(resolver.resolve(job)).isNull();
    }
}

