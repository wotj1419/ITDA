package com.itda.backend.scene.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.repository.ProjectMapper;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scene.controller.dto.request.CreateSceneRequest;
import com.itda.backend.scene.controller.dto.request.ReorderScenesRequest;
import com.itda.backend.scene.controller.dto.request.UpdateSceneRequest;
import com.itda.backend.scene.controller.dto.response.SceneCreateResponse;
import com.itda.backend.scene.controller.dto.response.SceneDetailResponse;
import com.itda.backend.scene.controller.dto.response.SceneSummaryResponse;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import com.itda.backend.scene.repository.dto.SceneSummary;
import com.itda.backend.scene.service.dto.SceneDraft;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SceneService {

    private final SceneMapper sceneMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectMapper projectMapper;

    @Transactional
    public SceneCreateResponse createScene(Long userId, Long projectId, CreateSceneRequest request) {
        requireProject(projectId);
        ensureMember(projectId, userId);

        int nextOrderIndex = sceneMapper.findNextOrderIndex(projectId);
        Scene scene = Scene.create(projectId, request.title(), request.description(), nextOrderIndex);

        sceneMapper.insertScene(scene);

        return new SceneCreateResponse(
                scene.getId(),
                scene.getTitle(),
                scene.getOrderIndex()
        );
    }

    @Transactional
    public List<SceneDetailResponse> createScenesAppend(Long userId, Long projectId, List<SceneDraft> drafts) {
        requireProject(projectId);
        ensureMember(projectId, userId);

        validateDrafts(drafts);
        int nextOrderIndex = sceneMapper.findNextOrderIndex(projectId);
        List<SceneDetailResponse> created = new ArrayList<>(drafts.size());
        int orderIndex = nextOrderIndex;

        for (SceneDraft draft : drafts) {
            Scene scene = Scene.create(projectId, draft.title(), draft.description(), orderIndex++);
            sceneMapper.insertScene(scene);
            created.add(SceneDetailResponse.from(scene));
        }

        return created;
    }

    @Transactional(readOnly = true)
    public List<SceneSummaryResponse> listScenes(Long userId, Long projectId) {
        requireProject(projectId);
        ensureMember(projectId, userId);

        List<SceneSummary> scenes = sceneMapper.findAllByProjectId(projectId);
        return scenes.stream()
                .map(SceneSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SceneDetailResponse getSceneDetail(Long userId, Long sceneId) {
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);
        return SceneDetailResponse.from(scene);
    }

    @Transactional
    public SceneDetailResponse updateScene(Long userId, Long sceneId, UpdateSceneRequest request) {
        validateUpdateRequest(request);
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);

        int updated = sceneMapper.updateScene(sceneId, request.title(), request.description());
        if (updated == 0) {
            throw new BusinessException(ErrorCode.SCENE_NOT_FOUND);
        }

        return buildUpdatedDetailResponse(scene, request);
    }

    @Transactional
    public void deleteScene(Long userId, Long sceneId) {
        Scene scene = requireScene(sceneId);
        ensureMember(scene.getProjectId(), userId);

        int deleted = sceneMapper.deleteScene(sceneId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.SCENE_NOT_FOUND);
        }
    }

    @Transactional
    public void reorderScenes(Long userId, Long projectId, ReorderScenesRequest request) {
        List<Long> orderedSceneIds = validateReorderRequest(request);
        requireProject(projectId);
        ensureMember(projectId, userId);

        // Full-list reorder 전제: 프로젝트의 전체 sceneId를 모두 포함해야 함.
        int totalScenes = sceneMapper.countByProjectId(projectId);
        if (totalScenes != orderedSceneIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        int matchedScenes = sceneMapper.countByProjectIdAndIds(projectId, orderedSceneIds);
        if (matchedScenes != orderedSceneIds.size()) {
            throw new BusinessException(ErrorCode.SCENE_NOT_FOUND);
        }

        sceneMapper.reorderScenes(projectId, orderedSceneIds);
    }
    
    // ========== Private Helper Methods ==========
    
    private void ensureMember(Long projectId, Long userId) {
        if (!projectMemberMapper.existsMember(projectId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private void requireProject(Long projectId) {
        projectMapper.findById(projectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PROJECT_NOT_FOUND));
    }

    private Scene requireScene(Long sceneId) {
        return sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
    }

    private void validateDrafts(List<SceneDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        for (SceneDraft draft : drafts) {
            if (draft == null) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    private void validateUpdateRequest(UpdateSceneRequest request) {
        if (request.title() == null && request.description() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private SceneDetailResponse buildUpdatedDetailResponse(Scene scene, UpdateSceneRequest request) {
        String title = request.title() != null ? request.title() : scene.getTitle();
        String description = request.description() != null ? request.description() : scene.getDescription();
        return new SceneDetailResponse(
                scene.getId(),
                scene.getProjectId(),
                title,
                description,
                scene.getOrderIndex()
        );
    }

    private List<Long> validateReorderRequest(ReorderScenesRequest request) {
        List<Long> orderedSceneIds = request.orderedSceneIds();
        if (orderedSceneIds == null || orderedSceneIds.isEmpty() || orderedSceneIds.contains(null)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        Set<Long> uniqueIds = new HashSet<>(orderedSceneIds);
        if (uniqueIds.size() != orderedSceneIds.size()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return orderedSceneIds;
    }
}
