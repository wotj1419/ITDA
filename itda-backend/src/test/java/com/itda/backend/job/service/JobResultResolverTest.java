package com.itda.backend.job.service;

import com.itda.backend.asset.config.S3StorageProperties;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.global.config.FileStorageProperties;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.node.repository.NodeMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobResultResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void resolve_ShouldReturnSceneExportUrl_WhenSceneMergeSucceeded() {
        long sceneId = 55L;
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(tempDir.toString());

        AssetMapper assetMapper = mock(AssetMapper.class);
        when(assetMapper.findById(anyLong())).thenReturn(Optional.empty());
        S3StorageProperties s3Properties = new S3StorageProperties();

        @SuppressWarnings("unchecked")
        ObjectProvider<S3Presigner> s3PresignerProvider = mock(ObjectProvider.class);
        when(s3PresignerProvider.getIfAvailable()).thenReturn(null);

        JobResultResolver resolver = new JobResultResolver(
                assetMapper,
                s3Properties,
                s3PresignerProvider,
                mock(NodeMapper.class),
                props,
                new MediaUrlResolver()
        );
        Job job = Job.builder()
                .id(1L)
                .type(JobType.SCENE_MERGE)
                .status(JobStatus.SUCCEEDED)
                .sceneId(sceneId)
                .build();

        assertThat(resolver.resolve(job)).isEqualTo("/api/scenes/55/export/file");
    }

    @Test
    void resolve_ShouldReturnNull_WhenJobNotSucceeded() {
        FileStorageProperties props = new FileStorageProperties();
        props.setUploadDir(tempDir.toString());

        AssetMapper assetMapper = mock(AssetMapper.class);
        when(assetMapper.findById(anyLong())).thenReturn(Optional.empty());
        S3StorageProperties s3Properties = new S3StorageProperties();

        @SuppressWarnings("unchecked")
        ObjectProvider<S3Presigner> s3PresignerProvider = mock(ObjectProvider.class);
        when(s3PresignerProvider.getIfAvailable()).thenReturn(null);

        JobResultResolver resolver = new JobResultResolver(
                assetMapper,
                s3Properties,
                s3PresignerProvider,
                mock(NodeMapper.class),
                props,
                new MediaUrlResolver()
        );
        Job job = Job.builder()
                .id(1L)
                .type(JobType.SCENE_MERGE)
                .status(JobStatus.FAILED)
                .sceneId(55L)
                .build();

        assertThat(resolver.resolve(job)).isNull();
    }
}
