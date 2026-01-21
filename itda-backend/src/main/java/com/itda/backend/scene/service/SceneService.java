package com.itda.backend.scene.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.repository.ProjectMemberMapper;
import com.itda.backend.scene.controller.dto.request.CreateSceneRequest;
import com.itda.backend.scene.controller.dto.response.SceneCreateResponse;
import com.itda.backend.scene.controller.dto.response.SceneDetailResponse;
import com.itda.backend.scene.controller.dto.response.SceneSummaryResponse;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import com.itda.backend.scene.repository.dto.SceneSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SceneService {

    private final SceneMapper sceneMapper;
    private final ProjectMemberMapper projectMemberMapper;

    @Transactional
    public SceneCreateResponse createScene(Long userId, Long projectId, CreateSceneRequest request) {
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

    @Transactional(readOnly = true)
    public List<SceneSummaryResponse> listScenes(Long userId, Long projectId) {
        ensureMember(projectId, userId);

        List<SceneSummary> scenes = sceneMapper.findAllByProjectId(projectId);
        return scenes.stream()
                .map(SceneSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SceneDetailResponse getSceneDetail(Long userId, Long sceneId) {
        Scene scene = sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        ensureMember(scene.getProjectId(), userId);
        return SceneDetailResponse.from(scene);
    }

    private void ensureMember(Long projectId, Long userId) {
        if (!projectMemberMapper.existsMember(projectId, userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
