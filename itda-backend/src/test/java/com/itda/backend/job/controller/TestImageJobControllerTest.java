package com.itda.backend.job.controller;

import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.job.service.JobResultResolver;
import com.itda.backend.job.service.JobService;
import com.itda.backend.node.repository.NodeMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TestImageJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class TestImageJobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobService jobService;

    @MockBean
    private JobResultResolver jobResultResolver;

    @MockBean
    private NodeMapper nodeMapper;

    @Test
    void createImageJob_returnsAcceptedJob() throws Exception {
        Job job = Job.builder()
                .id(123L)
                .projectId(1L)
                .type(JobType.IMAGE_GENERATION)
                .status(JobStatus.PENDING)
                .build();

        when(jobService.createAndEnqueue(
                eq(JobType.IMAGE_GENERATION),
                eq(1L),
                eq(null),
                eq(null),
                anyString(),
                eq(null),
                anyBoolean()
        )).thenReturn(job);

        when(jobResultResolver.resolve(job)).thenReturn(null);

        mockMvc.perform(post("/test/ai/image-jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"projectId\":1,\"prompt\":\"test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.jobId").value(123));
    }
}
