package com.itda.backend.project.service;

import com.itda.backend.auth.domain.User;
import com.itda.backend.auth.repository.UserMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.controller.dto.request.MemberInviteRequest;
import com.itda.backend.project.controller.dto.request.MemberRoleUpdateRequest;
import com.itda.backend.project.controller.dto.response.ProjectMemberResponse;
import com.itda.backend.project.repository.ProjectMemberMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private ProjectAccessService projectAccessService;

    @Mock
    private ProjectMemberMapper projectMemberMapper;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private MemberService memberService;

    @Test
    void getMembers_ShouldReturnMemberList() {
        Long userId = 1L;
        Long projectId = 10L;

        List<ProjectMemberResponse> members = List.of(
                new ProjectMemberResponse(1L, "owner@itda.com", "Owner", null, "OWNER"),
                new ProjectMemberResponse(2L, "admin@itda.com", "Admin", null, "ADMIN")
        );

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findAllMembers(projectId)).thenReturn(members);

        List<ProjectMemberResponse> result = memberService.getMembers(userId, projectId);

        assertThat(result).hasSize(2);
        verify(projectMemberMapper).findAllMembers(projectId);
    }

    @Test
    void inviteMember_ShouldRejectWhenUserNotFound() {
        Long userId = 1L;
        Long projectId = 10L;
        MemberInviteRequest request = new MemberInviteRequest("missing@itda.com", "EDITOR");

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("OWNER"));
        when(userMapper.findByEmail(request.email())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.inviteMember(userId, projectId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void inviteMember_ShouldRejectDuplicateMember() {
        Long userId = 1L;
        Long projectId = 10L;
        MemberInviteRequest request = new MemberInviteRequest("user@itda.com", "EDITOR");

        User invitee = User.builder()
                .id(2L)
                .email("user@itda.com")
                .name("User")
                .build();

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("OWNER"));
        when(userMapper.findByEmail(request.email())).thenReturn(Optional.of(invitee));
        when(projectMemberMapper.existsMember(projectId, invitee.getId())).thenReturn(true);

        assertThatThrownBy(() -> memberService.inviteMember(userId, projectId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MEMBER_ALREADY_EXISTS);
    }

    @Test
    void inviteMember_ShouldRejectAdminInvitingAdmin() {
        Long userId = 3L;
        Long projectId = 10L;
        MemberInviteRequest request = new MemberInviteRequest("admin@itda.com", "ADMIN");

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("ADMIN"));

        assertThatThrownBy(() -> memberService.inviteMember(userId, projectId, request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void updateRole_ShouldRejectAdminPromotingToAdmin() {
        Long userId = 2L;
        Long projectId = 10L;
        Long targetUserId = 3L;

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("ADMIN"));
        when(projectMemberMapper.findRole(projectId, targetUserId)).thenReturn(Optional.of("EDITOR"));

        assertThatThrownBy(() ->
                memberService.updateRole(userId, projectId, targetUserId, new MemberRoleUpdateRequest("ADMIN")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void updateRole_ShouldAllowAdminSelfDemotion() {
        Long userId = 2L;
        Long projectId = 10L;

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("ADMIN"));
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("ADMIN"));

        MemberRoleUpdateRequest request = new MemberRoleUpdateRequest("EDITOR");
        memberService.updateRole(userId, projectId, userId, request);

        verify(projectMemberMapper).updateMemberRole(projectId, userId, "EDITOR");
    }

    @Test
    void updateRole_ShouldRejectOwnerChangingOwnerRole() {
        Long userId = 1L;
        Long projectId = 10L;
        Long targetUserId = 1L;

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("OWNER"));
        when(projectMemberMapper.findRole(projectId, targetUserId)).thenReturn(Optional.of("OWNER"));

        assertThatThrownBy(() ->
                memberService.updateRole(userId, projectId, targetUserId, new MemberRoleUpdateRequest("ADMIN")))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }

    @Test
    void kickMember_ShouldRejectSelfKick() {
        Long userId = 2L;
        Long projectId = 10L;

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);

        assertThatThrownBy(() -> memberService.kickMember(userId, projectId, userId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_KICK_SELF);
    }

    @Test
    void kickMember_ShouldRejectAdminKickingAdmin() {
        Long userId = 2L;
        Long projectId = 10L;
        Long targetUserId = 3L;

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("ADMIN"));
        when(projectMemberMapper.findRole(projectId, targetUserId)).thenReturn(Optional.of("ADMIN"));

        assertThatThrownBy(() -> memberService.kickMember(userId, projectId, targetUserId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void leaveProject_ShouldRejectOwner() {
        Long userId = 1L;
        Long projectId = 10L;

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("OWNER"));

        assertThatThrownBy(() -> memberService.leaveProject(userId, projectId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.CANNOT_LEAVE_OWNER);
    }

    @Test
    void leaveProject_ShouldDeleteMember() {
        Long userId = 5L;
        Long projectId = 10L;

        doNothing().when(projectAccessService).ensureProjectAccessible(projectId, userId);
        when(projectMemberMapper.findRole(projectId, userId)).thenReturn(Optional.of("EDITOR"));

        memberService.leaveProject(userId, projectId);

        verify(projectMemberMapper).deleteMember(projectId, userId);
    }
}
