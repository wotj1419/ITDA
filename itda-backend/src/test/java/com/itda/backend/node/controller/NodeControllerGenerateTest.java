package com.itda.backend.node.controller;

import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.global.security.JwtTokenProvider;
import com.itda.backend.job.domain.Job;
import com.itda.backend.job.domain.JobStatus;
import com.itda.backend.job.domain.JobType;
import com.itda.backend.media.MediaFileService;
import com.itda.backend.node.controller.dto.request.GenerateNodeRequest;
import com.itda.backend.node.service.NodeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NodeController.class)
@Import({JacksonAutoConfiguration.class, NodeControllerGenerateTest.TestWebConfig.class})
@AutoConfigureMockMvc(addFilters = false)
class NodeControllerGenerateTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NodeService nodeService;

    @MockBean
    private MediaFileService mediaFileService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void generateNode_acceptsReferenceObjectIds() throws Exception {
        Job job = Job.builder()
                .id(123L)
                .projectId(1L)
                .type(JobType.IMAGE_GENERATION)
                .status(JobStatus.PENDING)
                .build();

        when(nodeService.generateNode(eq(1L), eq(10L), any(GenerateNodeRequest.class)))
                .thenReturn(job);

        String requestBody = """
                {
                  "prompt": "test prompt",
                  "settings": {"aspectRatio": "1:1"},
                  "referenceObjectIds": [1, 2]
                }
                """;

        mockMvc.perform(post("/api/nodes/10/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value("ACCEPTED"))
                .andExpect(jsonPath("$.data.jobId").value(123));

        ArgumentCaptor<GenerateNodeRequest> captor = ArgumentCaptor.forClass(GenerateNodeRequest.class);
        verify(nodeService).generateNode(eq(1L), eq(10L), captor.capture());
        assertThat(captor.getValue().referenceObjectIds()).containsExactly(1L, 2L);
    }

    @TestConfiguration
    static class TestWebConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                            && CustomUserDetails.class.isAssignableFrom(parameter.getParameterType());
                }

                @Override
                public Object resolveArgument(
                        MethodParameter parameter,
                        ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest,
                        WebDataBinderFactory binderFactory
                ) {
                    return new CustomUserDetails(1L, "tester@example.com", "USER");
                }
            });
        }
    }
}
