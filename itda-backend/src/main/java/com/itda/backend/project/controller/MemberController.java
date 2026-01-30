package com.itda.backend.project.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.project.controller.dto.request.MemberInviteRequest;
import com.itda.backend.project.controller.dto.request.MemberRoleUpdateRequest;
import com.itda.backend.project.controller.dto.response.ProjectMemberResponse;
import com.itda.backend.project.controller.dto.response.RoleChangeResponse;
import com.itda.backend.project.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "프로젝트 멤버", description = "프로젝트 멤버 관리 API")
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "멤버 목록 조회", description = "프로젝트에 참여 중인 멤버 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProjectMemberResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트를 찾을 수 없음")
    })
    @GetMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<List<ProjectMemberResponse>>> getMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId
    ) {
        List<ProjectMemberResponse> response = memberService.getMembers(userDetails.getUserId(), projectId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "멤버 초대", description = "이메일로 사용자를 검색하여 멤버로 초대합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "초대 성공",
                    content = @Content(schema = @Schema(implementation = ProjectMemberResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자/프로젝트 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 멤버")
    })
    @PostMapping("/{projectId}/members")
    public ResponseEntity<ApiResponse<ProjectMemberResponse>> inviteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Valid @RequestBody MemberInviteRequest request
    ) {
        ProjectMemberResponse response = memberService.inviteMember(userDetails.getUserId(), projectId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "멤버 권한 변경", description = "프로젝트 멤버의 권한을 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 성공",
                    content = @Content(schema = @Schema(implementation = RoleChangeResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 없음")
    })
    @PatchMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ApiResponse<RoleChangeResponse>> updateMemberRole(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "대상 사용자 ID") @PathVariable Long userId,
            @Valid @RequestBody MemberRoleUpdateRequest request
    ) {
        RoleChangeResponse response = memberService.updateRole(userDetails.getUserId(), projectId, userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "멤버 강퇴", description = "프로젝트 멤버를 강퇴합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "강퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 없음")
    })
    @DeleteMapping("/{projectId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId,
            @Parameter(description = "대상 사용자 ID") @PathVariable Long userId
    ) {
        memberService.kickMember(userDetails.getUserId(), projectId, userId);
        return ApiResponse.success();
    }

    @Operation(summary = "프로젝트 탈퇴", description = "프로젝트에서 탈퇴합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로젝트 없음")
    })
    @DeleteMapping("/{projectId}/members/me")
    public ResponseEntity<ApiResponse<Void>> leaveProject(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "프로젝트 ID") @PathVariable Long projectId
    ) {
        memberService.leaveProject(userDetails.getUserId(), projectId);
        return ApiResponse.success();
    }
}
