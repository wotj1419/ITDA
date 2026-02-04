package com.itda.backend.project.controller;

import com.itda.backend.global.response.ApiResponse;
import com.itda.backend.global.security.CustomUserDetails;
import com.itda.backend.project.controller.dto.request.MemberInviteRequest;
import com.itda.backend.project.controller.dto.response.ProjectInviteResponse;
import com.itda.backend.project.service.ProjectInviteService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Invites", description = "Project invite APIs")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InviteController {

    private final ProjectInviteService projectInviteService;

    @Operation(summary = "List my invites", description = "Returns all invites for the authenticated user.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Invites retrieved",
                    content = @Content(schema = @Schema(implementation = ProjectInviteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/invites")
    public ResponseEntity<ApiResponse<List<ProjectInviteResponse>>> getInvites(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ProjectInviteResponse> response = projectInviteService.getInvites(userDetails.getUserId());
        return ApiResponse.success(response);
    }

    @Operation(summary = "Create project invite", description = "Create a pending invite for a project member.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Invite created",
                    content = @Content(schema = @Schema(implementation = ProjectInviteResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    @PostMapping("/projects/{projectId}/invites")
    public ResponseEntity<ApiResponse<ProjectInviteResponse>> createInvite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Project ID") @PathVariable Long projectId,
            @Valid @RequestBody MemberInviteRequest request
    ) {
        ProjectInviteResponse response = projectInviteService.createInvite(
                userDetails.getUserId(),
                projectId,
                request
        );
        return ApiResponse.created(response);
    }

    @Operation(summary = "Accept invite", description = "Accept a pending invite.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invite accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    @PostMapping("/invites/{inviteId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Invite ID") @PathVariable Long inviteId
    ) {
        projectInviteService.acceptInvite(userDetails.getUserId(), inviteId);
        return ApiResponse.success();
    }

    @Operation(summary = "Decline invite", description = "Decline a pending invite.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invite declined"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflict")
    })
    @PostMapping("/invites/{inviteId}/decline")
    public ResponseEntity<ApiResponse<Void>> declineInvite(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "Invite ID") @PathVariable Long inviteId
    ) {
        projectInviteService.declineInvite(userDetails.getUserId(), inviteId);
        return ApiResponse.success();
    }
}
