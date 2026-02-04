package com.itda.backend.project.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.controller.dto.request.MemberRoleUpdateRequest;
import com.itda.backend.project.controller.dto.response.ProjectMemberResponse;
import com.itda.backend.project.controller.dto.response.RoleChangeResponse;
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
public class MemberService {

    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_EDITOR = "EDITOR";
    private static final String ROLE_VIEWER = "VIEWER";

    private static final Set<String> ALL_ROLES = Set.of(
            ROLE_OWNER, ROLE_ADMIN, ROLE_EDITOR, ROLE_VIEWER
    );
    private final ProjectAccessService projectAccessService;
    private final ProjectMemberMapper projectMemberMapper;

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getMembers(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        return projectMemberMapper.findAllMembers(projectId);
    }

    @Transactional
    public RoleChangeResponse updateRole(Long userId,
                                         Long projectId,
                                         Long targetUserId,
                                         MemberRoleUpdateRequest request) {
        projectAccessService.ensureProjectAccessible(projectId, userId);

        String actorRole = requireMemberRole(projectId, userId);
        if (!isOwnerOrAdmin(actorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String targetRole = requireTargetRole(projectId, targetUserId);
        String newRole = normalizeRole(request.role());
        validateUpdateRole(newRole);
        validateRoleChangePermission(actorRole, userId, targetUserId, targetRole, newRole);

        if (!targetRole.equals(newRole)) {
            projectMemberMapper.updateMemberRole(projectId, targetUserId, newRole);
        }

        return new RoleChangeResponse(targetUserId, targetRole, newRole);
    }

    @Transactional
    public void kickMember(Long userId, Long projectId, Long targetUserId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);

        if (userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.CANNOT_KICK_SELF);
        }

        String actorRole = requireMemberRole(projectId, userId);
        if (!isOwnerOrAdmin(actorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        String targetRole = requireTargetRole(projectId, targetUserId);
        validateKickPermission(actorRole, targetRole);

        projectMemberMapper.deleteMember(projectId, targetUserId);
    }

    @Transactional
    public void leaveProject(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);

        String role = requireMemberRole(projectId, userId);
        if (ROLE_OWNER.equals(role)) {
            throw new BusinessException(ErrorCode.CANNOT_LEAVE_OWNER);
        }

        projectMemberMapper.deleteMember(projectId, userId);
    }

    private String requireMemberRole(Long projectId, Long userId) {
        return projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private String requireTargetRole(Long projectId, Long userId) {
        return projectMemberMapper.findRole(projectId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FORBIDDEN));
    }

    private void validateUpdateRole(String role) {
        if (!ALL_ROLES.contains(role)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateRoleChangePermission(String actorRole,
                                              Long actorUserId,
                                              Long targetUserId,
                                              String targetRole,
                                              String newRole) {
        if (ROLE_OWNER.equals(targetRole) && !ROLE_OWNER.equals(actorRole)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        if (ROLE_OWNER.equals(targetRole) && ROLE_OWNER.equals(actorRole) && !ROLE_OWNER.equals(newRole)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (ROLE_ADMIN.equals(actorRole)) {
            if (actorUserId.equals(targetUserId)) {
                if (ROLE_EDITOR.equals(newRole) || ROLE_VIEWER.equals(newRole)) {
                    return;
                }
            }
            if (ROLE_ADMIN.equals(targetRole) || ROLE_OWNER.equals(targetRole)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            if (ROLE_ADMIN.equals(newRole)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
        }
    }

    private void validateKickPermission(String actorRole, String targetRole) {
        if (ROLE_OWNER.equals(actorRole)) {
            if (ROLE_OWNER.equals(targetRole)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        if (ROLE_ADMIN.equals(actorRole)) {
            if (ROLE_ADMIN.equals(targetRole) || ROLE_OWNER.equals(targetRole)) {
                throw new BusinessException(ErrorCode.FORBIDDEN);
            }
            return;
        }
        throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    private boolean isOwnerOrAdmin(String role) {
        return ROLE_OWNER.equals(role) || ROLE_ADMIN.equals(role);
    }

    private String normalizeRole(String role) {
        return Optional.ofNullable(role)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .orElse("");
    }
}
