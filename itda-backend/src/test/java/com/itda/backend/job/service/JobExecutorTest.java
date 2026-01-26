package com.itda.backend.job.service;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.repository.JobMapper;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.repository.NodeMapper;
import com.itda.backend.worker.ExecutionResult;
import com.itda.backend.worker.image.ImageGenerationWorker;
import com.itda.backend.worker.merge.MergeWorker;
import com.itda.backend.worker.video.VideoGenerationWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobExecutorTest {

    @Mock
    private JobMapper jobMapper;

    @Mock
    private NodeMapper nodeMapper;

    @Mock
    private JobEventPublisher jobEventPublisher;

    @Mock
    private ImageGenerationWorker imageWorker;

    @Mock
    private VideoGenerationWorker videoWorker;

    @Mock
    private MergeWorker mergeWorker;

    @Mock
    private Environment environment;

    private JobExecutionProperties jobExecutionProperties;
    @Mock
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        jobExecutionProperties = new JobExecutionProperties();
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        lenient().when(environment.getActiveProfiles()).thenReturn(new String[0]);
    }

    private JobExecutor newExecutor() {
        return new JobExecutor(
                jobMapper,
                nodeMapper,
                jobEventPublisher,
                transactionTemplate,
                jobExecutionProperties,
                imageWorker,
                videoWorker,
                mergeWorker,
                environment
        );
    }

    @Test
    void execute_ShouldSkip_WhenJobIdIsNull() {
        JobExecutor executor = newExecutor();

        executor.execute(null);

        verifyNoInteractions(jobMapper, nodeMapper, jobEventPublisher, imageWorker, videoWorker, mergeWorker);
    }

    @Test
    void execute_ShouldSkip_WhenJobNotFound() {
        JobExecutor executor = newExecutor();
        when(jobMapper.findById(1L)).thenReturn(Optional.empty());

        executor.execute(1L);

        verify(jobMapper).findById(1L);
        verifyNoInteractions(nodeMapper, jobEventPublisher, imageWorker, videoWorker, mergeWorker);
    }

    @Test
    void execute_ShouldSkip_WhenJobAlreadySucceeded() {
        JobExecutor executor = newExecutor();
        Job job = Job.builder()
                .id(1L)
                .type(JobType.IMAGE_GENERATION)
                .status(JobStatus.SUCCEEDED)
                .build();
        when(jobMapper.findById(1L)).thenReturn(Optional.of(job));

        executor.execute(1L);

        verify(jobMapper).findById(1L);
        verify(jobMapper, never()).updateStatusIfExpected(anyLong(), any(), any(), any(), anyInt());
        verifyNoInteractions(nodeMapper, jobEventPublisher, imageWorker, videoWorker, mergeWorker);
    }

    @Test
    void execute_ShouldSkip_WhenJobNotExecutable() {
        JobExecutor executor = newExecutor();
        jobExecutionProperties.setMaxRetryCount(1);

        Job job = Job.builder()
                .id(1L)
                .type(JobType.VIDEO_GENERATION)
                .status(JobStatus.FAILED)
                .retryCount(1)
                .build();
        when(jobMapper.findById(1L)).thenReturn(Optional.of(job));

        executor.execute(1L);

        verify(jobMapper).findById(1L);
        verify(jobMapper, never()).updateStatusIfExpected(anyLong(), any(), any(), any(), anyInt());
        verifyNoInteractions(nodeMapper, jobEventPublisher, imageWorker, videoWorker, mergeWorker);
    }

    @Test
    void execute_ShouldSkip_WhenFailedToMarkRunning() {
        JobExecutor executor = newExecutor();
        Job job = Job.builder()
                .id(1L)
                .type(JobType.IMAGE_GENERATION)
                .status(JobStatus.PENDING)
                .nodeId(100L)
                .build();
        when(jobMapper.findById(1L)).thenReturn(Optional.of(job));
        when(jobMapper.updateStatusIfExpected(
                eq(1L),
                eq(List.of(JobStatus.PENDING, JobStatus.FAILED)),
                eq(JobStatus.RUNNING),
                eq(null),
                anyInt()
        )).thenReturn(0);

        executor.execute(1L);

        verify(jobMapper).updateStatusIfExpected(eq(1L), any(), eq(JobStatus.RUNNING), eq(null), anyInt());
        verifyNoInteractions(nodeMapper, jobEventPublisher, imageWorker, videoWorker, mergeWorker);
    }

    @Test
    void execute_ImageGeneration_ShouldUpdateNodeAndPublishDone_OnSuccess() {
        JobExecutor executor = newExecutor();
        Job job = Job.builder()
                .id(1L)
                .projectId(10L)
                .nodeId(100L)
                .type(JobType.IMAGE_GENERATION)
                .status(JobStatus.PENDING)
                .build();
        when(jobMapper.findById(1L)).thenReturn(Optional.of(job));
        when(jobMapper.updateStatusIfExpected(anyLong(), any(), any(), any(), anyInt())).thenReturn(1);
        when(nodeMapper.updateStatus(100L, NodeStatus.RUNNING)).thenReturn(1);
        when(imageWorker.execute(job)).thenReturn(new ExecutionResult(123L, "ai/images/123.png"));
        when(jobMapper.updateResultIfRunning(1L, 123L)).thenReturn(1);
        when(nodeMapper.updateStatusAndContentUrl(100L, NodeStatus.SUCCEEDED, "ai/images/123.png")).thenReturn(1);

        executor.execute(1L);

        verify(imageWorker).execute(job);
        verify(jobMapper).updateResultIfRunning(1L, 123L);
        verify(jobEventPublisher).publishDone(1L);
        verify(jobEventPublisher, never()).publishFailed(1L);
    }

    @Test
    void execute_ShouldMarkFailedAndPublishFailed_WhenWorkerReturnsNull() {
        JobExecutor executor = newExecutor();
        Job job = Job.builder()
                .id(1L)
                .projectId(10L)
                .nodeId(200L)
                .type(JobType.VIDEO_GENERATION)
                .status(JobStatus.PENDING)
                .build();
        when(jobMapper.findById(1L)).thenReturn(Optional.of(job));
        when(jobMapper.updateStatusIfExpected(anyLong(), any(), any(), any(), anyInt())).thenReturn(1);
        when(nodeMapper.updateStatus(200L, NodeStatus.RUNNING)).thenReturn(1);
        when(videoWorker.execute(job)).thenReturn(null);
        when(jobMapper.updateFailureIfRunning(eq(1L), any())).thenReturn(1);
        when(nodeMapper.updateStatus(200L, NodeStatus.FAILED)).thenReturn(1);

        executor.execute(1L);

        verify(videoWorker).execute(job);
        verify(jobMapper).updateFailureIfRunning(eq(1L), any());
        verify(jobEventPublisher).publishFailed(1L);
        verify(jobEventPublisher, never()).publishDone(1L);
    }

    @Test
    void execute_LocalProfile_ShouldUseRootCauseMessage_ForPersistedError() {
        when(environment.getActiveProfiles()).thenReturn(new String[]{"local"});
        JobExecutor executor = newExecutor();
        Job job = Job.builder()
                .id(1L)
                .projectId(10L)
                .nodeId(100L)
                .type(JobType.IMAGE_GENERATION)
                .status(JobStatus.PENDING)
                .build();
        when(jobMapper.findById(1L)).thenReturn(Optional.of(job));
        when(jobMapper.updateStatusIfExpected(anyLong(), any(), any(), any(), anyInt())).thenReturn(1);
        when(nodeMapper.updateStatus(100L, NodeStatus.RUNNING)).thenReturn(1);
        when(imageWorker.execute(job))
                .thenThrow(new IllegalStateException("outer", new RuntimeException("root-cause")));
        when(jobMapper.updateFailureIfRunning(1L, "root-cause")).thenReturn(1);
        when(nodeMapper.updateStatus(100L, NodeStatus.FAILED)).thenReturn(1);

        executor.execute(1L);

        verify(jobMapper).updateFailureIfRunning(1L, "root-cause");
        verify(jobEventPublisher).publishFailed(1L);
    }
}
