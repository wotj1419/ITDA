package com.itda.backend.project.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 프로젝트 접근 권한 검증 서비스
 */
@Service
@RequiredArgsConstructor
public class ProjectAccessService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;

    public void ensureProjectAccessible(Long projectId, Long userId) {
        if (projectMapper.findById(projectId).isEmpty()) {
            throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
        }
        if (!projectMemberMapper.existsMember(projectId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    // 협업 WS 등 다른 모듈에서 사용할 공용 체크 API
    @SuppressWarnings("unused")
    public boolean isProjectMember(Long projectId, Long userId) {
        return projectMemberMapper.existsMember(projectId, userId);
    }
}
