package com.itda.backend.project.service;

import com.itda.backend.auth.domain.User;
import com.itda.backend.auth.repository.UserMapper;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.controller.dto.request.MemberInviteRequest;
import com.itda.backend.project.controller.dto.response.ProjectInviteResponse;
import com.itda.backend.project.domain.ProjectInvite;
import com.itda.backend.project.repository.ProjectInviteMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectInviteService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EDITOR = "EDITOR";
    private static final String ROLE_VIEWER = "VIEWER";

    private static final Set<String> INVITE_ROLES = Set.of(
            ROLE_ADMIN, ROLE_EDITOR, ROLE_VIEWER
    );

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ACCEPTED = "ACCEPTED";
    private static final String STATUS_DECLINED = "DECLINED";

    private final ProjectAccessService projectAccessService;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectInviteMapper projectInviteMapper;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public List<ProjectInviteResponse> getInvites(Long userId) {
        return projectInviteMapper.findAllByReceiverId(userId);
    }

    @Transactional
    public ProjectInviteResponse createInvite(Long userId, Long projectId, MemberInviteRequest request) {
        projectAccessService.ensureProjectAccessible(projectId, userId);

        String actorRole = requireMemberRole(projectId, userId);
        String inviteRole = normalizeRole(request.role());
        validateInviteRole(inviteRole);
        validateInvitePermission(actorRole, inviteRole);

        User receiver = userMapper.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (projectMemberMapper.existsMember(projectId, receiver.getId())) {
            throw new BusinessException(ErrorCode.MEMBER_ALREADY_EXISTS);
        }

        Optional<ProjectInvite> existing = projectInviteMapper.findByProjectIdAndReceiverId(projectId, receiver.getId());
        if (existing.isPresent()) {
            ProjectInvite invite = existing.get();
            if (STATUS_PENDING.equals(invite.getStatus())) {
                throw new BusinessException(ErrorCode.INVITE_ALREADY_EXISTS);
            }
            projectInviteMapper.updateInviteForResend(invite.getId(), userId, inviteRole);
            return requireResponse(invite.getId());
        }

        ProjectInvite invite = ProjectInvite.builder()
                .projectId(projectId)
                .senderId(userId)
                .receiverId(receiver.getId())
                .role(inviteRole)
                .status(STATUS_PENDING)
                .build();
        projectInviteMapper.insertInvite(invite);
        return requireResponse(invite.getId());
    }

    @Transactional
    public void acceptInvite(Long userId, Long inviteId) {
        ProjectInvite invite = projectInviteMapper.findById(inviteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));

        if (!invite.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (STATUS_DECLINED.equals(invite.getStatus())) {
            throw new BusinessException(ErrorCode.INVITE_STATUS_CONFLICT);
        }

        if (STATUS_ACCEPTED.equals(invite.getStatus())) {
            return;
        }

        if (!projectMemberMapper.existsMember(invite.getProjectId(), userId)) {
            projectMemberMapper.insertMember(invite.getProjectId(), userId, invite.getRole());
        }
        projectInviteMapper.updateInviteStatus(inviteId, STATUS_ACCEPTED);
    }

    @Transactional
    public void declineInvite(Long userId, Long inviteId) {
        ProjectInvite invite = projectInviteMapper.findById(inviteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));

        if (!invite.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        if (STATUS_ACCEPTED.equals(invite.getStatus())) {
            throw new BusinessException(ErrorCode.INVITE_STATUS_CONFLICT);
        }

        if (STATUS_DECLINED.equals(invite.getStatus())) {
            return;
        }

        projectInviteMapper.updateInviteStatus(inviteId, STATUS_DECLINED);
    }

    private ProjectInviteResponse requireResponse(Long inviteId) {
        return projectInviteMapper.findResponseById(inviteId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR));
    }

    private String requireMemberRole(Long projectId, Long userId) {
        return projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private void validateInviteRole(String role) {
        if (!INVITE_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateInvitePermission(String actorRole, String inviteRole) {
        if (ROLE_OWNER.equals(actorRole)) {
            return;
        }
        if (ROLE_ADMIN.equals(actorRole) && !ROLE_ADMIN.equals(inviteRole)) {
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private String normalizeRole(String role) {
        return Optional.ofNullable(role)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .orElse("");
    }
}
