package com.itda.backend.scenario.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.scenario.controller.dto.request.GeneratePromptRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePlotRequest;
import com.itda.backend.scenario.controller.dto.request.UpdatePromptRequest;
import com.itda.backend.scenario.controller.dto.response.ScenarioPlotResponse;
import com.itda.backend.scenario.controller.dto.response.ScenarioPromptResponse;
import com.itda.backend.scenario.controller.dto.response.ScenarioResponse;
import com.itda.backend.scenario.service.ScenarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Scenario", description = "시나리오 생성/수정 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ScenarioController {

    private final ScenarioService scenarioService;

    @Operation(
            summary = "시나리오 조회",
            description = "프로젝트의 시나리오 정보를 조회합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ScenarioResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 또는 시나리오를 찾을 수 없음"
            )
    })
    @GetMapping("/projects/{projectId}/scenario")
    public ResponseEntity<ApiResponse<ScenarioResponse>> getScenario(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ScenarioResponse response = scenarioService.getScenario(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프롬프트 생성",
            description = "입력 정보를 바탕으로 AI 프롬프트를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = ScenarioPromptResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "시나리오 버전 충돌"
            )
    })
    @PostMapping("/projects/{projectId}/scenario/prompt/generate")
    public ResponseEntity<ApiResponse<ScenarioPromptResponse>> generatePrompt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody GeneratePromptRequest request) {
        ScenarioPromptResponse response = scenarioService.generatePrompt(userDetails.getUserId(), projectId, request);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "프롬프트 수정",
            description = "사용자가 프롬프트를 수정하고 상태(DRAFT/APPROVED)를 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 또는 시나리오를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "시나리오 버전 충돌"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "시나리오 상태가 올바르지 않음"
            )
    })
    @PutMapping("/projects/{projectId}/scenario/prompt")
    public ResponseEntity<ApiResponse<Void>> updatePrompt(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdatePromptRequest request) {
        scenarioService.updatePrompt(userDetails.getUserId(), projectId, request);
        return ApiResponse.success();
    }

    @Operation(
            summary = "줄거리 생성",
            description = "승인된 프롬프트를 바탕으로 줄거리를 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "생성 성공",
                    content = @Content(schema = @Schema(implementation = ScenarioPlotResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 또는 시나리오를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "시나리오 버전 충돌"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "시나리오 상태가 올바르지 않음"
            )
    })
    @PostMapping("/projects/{projectId}/scenario/plot/generate")
    public ResponseEntity<ApiResponse<ScenarioPlotResponse>> generatePlot(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId) {
        ScenarioPlotResponse response = scenarioService.generatePlot(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(
            summary = "줄거리 수정",
            description = "사용자가 줄거리를 수정하고 상태(DRAFT/APPROVED)를 변경합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "수정 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "입력 값 검증 실패"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 필요"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "프로젝트 접근 권한 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "프로젝트 또는 시나리오를 찾을 수 없음"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "시나리오 버전 충돌"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "시나리오 상태가 올바르지 않음"
            )
    })
    @PutMapping("/projects/{projectId}/scenario/plot")
    public ResponseEntity<ApiResponse<Void>> updatePlot(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long projectId,
            @Valid @RequestBody UpdatePlotRequest request) {
        scenarioService.updatePlot(userDetails.getUserId(), projectId, request);
        return ApiResponse.success();
    }
}
