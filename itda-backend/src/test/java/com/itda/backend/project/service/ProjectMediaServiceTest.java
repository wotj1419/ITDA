package com.itda.backend.project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itda.backend.asset.service.AssetUrlResolver;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.service.JobCreateRequest;
import com.itda.backend.job.service.JobService;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.node.repository.dto.TimelineNodeRow;
import com.itda.backend.project.controller.dto.response.ProjectTimelineResponse;
import com.itda.backend.timeline.controller.dto.response.MergeResponse;
import com.itda.backend.timeline.domain.ProjectMerge;
import com.itda.backend.timeline.repository.ProjectMergeMapper;
import com.itda.backend.timeline.repository.TimelineMapper;
import com.itda.backend.timeline.service.MergeSignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectMediaServiceTest {

    private static final Long USER_ID = 3L;
    private static final Long PROJECT_ID = 5L;

    @Mock
    private ProjectAccessService projectAccessService;
    @Mock
    private TimelineMapper timelineMapper;
    @Mock
    private MergeSignatureService mergeSignatureService;
    @Mock
    private JobService jobService;
    @Mock
    private MediaUrlResolver mediaUrlResolver;
    @Mock
    private ProjectMergeMapper projectMergeMapper;
    @Mock
    private AssetUrlResolver assetUrlResolver;

    private ProjectMediaService projectMediaService;

    @BeforeEach
    void setUp() {
        projectMediaService = new ProjectMediaService(
                projectAccessService,
                timelineMapper,
                mergeSignatureService,
                jobService,
                new ObjectMapper(),
                mediaUrlResolver,
                projectMergeMapper,
                assetUrlResolver);
    }

    @Test
    void reorderTimeline_updatesOrderUsingVideoNodeIds() {
        List<Long> orderedVideoNodeIds = List.of(21L, 16L, 30L);
        when(timelineMapper.countProjectTimelineItems(PROJECT_ID)).thenReturn(3);
        when(timelineMapper.countProjectTimelineItemsByVideoNodeIds(PROJECT_ID, orderedVideoNodeIds)).thenReturn(3);

        projectMediaService.reorderTimeline(USER_ID, PROJECT_ID, orderedVideoNodeIds);

        verify(projectAccessService).ensureProjectAccessible(PROJECT_ID, USER_ID);
        verify(timelineMapper).reorderProjectTimelineItemsByVideoNodeIds(PROJECT_ID, orderedVideoNodeIds);
    }

    @Test
    void reorderTimeline_throwsWhenOrderedIdsDoNotMatchTimelineItems() {
        List<Long> orderedVideoNodeIds = List.of(21L, 16L);
        when(timelineMapper.countProjectTimelineItems(PROJECT_ID)).thenReturn(3);
        when(timelineMapper.countProjectTimelineItemsByVideoNodeIds(PROJECT_ID, orderedVideoNodeIds)).thenReturn(2);

        assertThatThrownBy(() -> projectMediaService.reorderTimeline(USER_ID, PROJECT_ID, orderedVideoNodeIds))
                .isInstanceOf(BusinessException.class);

        verify(timelineMapper, never()).reorderProjectTimelineItemsByVideoNodeIds(any(), any());
    }

    @Test
    void requestMerge_usesProjectTimelineOrderAsMergeInput() {
        TimelineNodeRow first = new TimelineNodeRow(21L, 6L, 1, 4, 101L, "video-21.mp4", null, null, null, null);
        TimelineNodeRow second = new TimelineNodeRow(16L, 6L, 2, 6, 102L, "video-16.mp4", null, null, null, null);
        List<TimelineNodeRow> orderedRows = List.of(first, second);

        when(timelineMapper.findProjectTimelineVideoNodes(PROJECT_ID)).thenReturn(orderedRows);
        when(mergeSignatureService.computeProjectSignatureFromVideoNodes(PROJECT_ID, false, orderedRows))
                .thenReturn("signature-by-order");
        when(projectMergeMapper.findActiveByProjectIdAndSignature(PROJECT_ID, "signature-by-order"))
                .thenReturn(Optional.empty());
        when(jobService.createAndEnqueue(any(JobCreateRequest.class), eq(true)))
                .thenReturn(Job.builder().id(99L).status(JobStatus.PENDING).type(JobType.PROJECT_MERGE).build());

        MergeResponse response = projectMediaService.requestMerge(USER_ID, PROJECT_ID);

        ArgumentCaptor<List<TimelineNodeRow>> rowsCaptor = ArgumentCaptor.forClass(List.class);
        verify(mergeSignatureService).computeProjectSignatureFromVideoNodes(eq(PROJECT_ID), eq(false), rowsCaptor.capture());
        assertThat(rowsCaptor.getValue()).extracting(TimelineNodeRow::getVideoNodeId)
                .containsExactly(21L, 16L);
        assertThat(response.cached()).isFalse();
        assertThat(response.jobId()).isEqualTo(99L);
    }

    @Test
    void getTimeline_includesDurationFromTimelineSource() {
        TimelineNodeRow row = new TimelineNodeRow(
                30L,
                11L,
                1,
                7,
                201L,
                "videos/30.mp4",
                301L,
                "images/shot-301.png",
                401L,
                "images/master-401.png");
        when(timelineMapper.findProjectTimelineVideoNodes(PROJECT_ID)).thenReturn(List.of(row));
        when(mediaUrlResolver.nodeContentUrl(30L, "videos/30.mp4")).thenReturn("/api/nodes/30/content");
        when(assetUrlResolver.resolvePublicUrl(201L, "videos/30.mp4")).thenReturn("https://cdn/video-30.mp4");
        when(assetUrlResolver.resolvePublicUrl(301L, "images/shot-301.png")).thenReturn("https://cdn/shot-301.png");
        when(assetUrlResolver.resolvePublicUrl(401L, "images/master-401.png")).thenReturn("https://cdn/master-401.png");

        ProjectTimelineResponse response = projectMediaService.getTimeline(USER_ID, PROJECT_ID);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).duration()).isEqualTo(7);
    }
}
