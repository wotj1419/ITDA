package com.itda.backend.scene.service;

import com.itda.backend.asset.service.AssetUrlResolver;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.project.service.ProjectAccessService;
import com.itda.backend.scene.controller.dto.response.SceneExportResponse;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import com.itda.backend.timeline.domain.SceneVideo;
import com.itda.backend.timeline.repository.SceneVideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SceneMediaService {

    private final SceneMapper sceneMapper;
    private final SceneVideoMapper sceneVideoMapper;
    private final ProjectAccessService projectAccessService;
    private final MediaUrlResolver mediaUrlResolver;
    private final AssetUrlResolver assetUrlResolver;

    @Transactional(readOnly = true)
    public SceneExportResponse getExport(Long userId, Long sceneId) {
        Scene scene = sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        projectAccessService.ensureProjectAccessible(scene.getProjectId(), userId);

        SceneVideo activeSceneVideo = sceneVideoMapper.findActiveBySceneId(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPORT_NOT_FOUND,
                        "No active scene merge result"));

        String fallbackUrl = mediaUrlResolver.sceneExportUrl(sceneId);
        String downloadUrl = assetUrlResolver.resolveDownloadUrl(
                activeSceneVideo.getAssetId(),
                fallbackUrl,
                "scene-" + sceneId + ".mp4"
        );

        return new SceneExportResponse(
                activeSceneVideo.getAssetId(),
                downloadUrl,
                activeSceneVideo.getMergeSignature(),
                activeSceneVideo.getStatus()
        );
    }
}
