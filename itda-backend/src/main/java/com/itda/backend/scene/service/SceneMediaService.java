package com.itda.backend.scene.service;

import com.itda.backend.asset.service.AssetUrlResolver;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.media.MediaUrlResolver;
import com.itda.backend.project.service.ProjectAccessService;
import com.itda.backend.scene.controller.dto.response.SceneExportResponse;
import com.itda.backend.scene.controller.dto.response.SceneExportItemResponse;
import com.itda.backend.scene.controller.dto.response.SceneExportListResponse;
import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.SceneMapper;
import com.itda.backend.timeline.domain.SceneVideo;
import com.itda.backend.timeline.repository.SceneVideoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SceneMediaService {

    private final SceneMapper sceneMapper;
    private final SceneVideoMapper sceneVideoMapper;
    private final ProjectAccessService projectAccessService;
    private final MediaUrlResolver mediaUrlResolver;
    private final AssetUrlResolver assetUrlResolver;
    private static final int DEFAULT_EXPORT_PAGE_SIZE = 8;
    private static final int MAX_EXPORT_PAGE_SIZE = 100;

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

    @Transactional(readOnly = true)
    public SceneExportListResponse listExports(Long userId, Long sceneId, Integer page, Integer size) {
        Scene scene = sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        projectAccessService.ensureProjectAccessible(scene.getProjectId(), userId);

        int safePage = page == null ? 1 : Math.max(page, 1);
        int safeSize = size == null ? DEFAULT_EXPORT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_EXPORT_PAGE_SIZE);
        int totalCount = sceneVideoMapper.countBySceneId(sceneId);
        int totalPages = totalCount == 0 ? 0 : (int) Math.ceil((double) totalCount / safeSize);
        int offset = (safePage - 1) * safeSize;
        List<SceneVideo> videos = sceneVideoMapper.findBySceneIdPaged(sceneId, safeSize, offset);
        List<SceneExportItemResponse> items = mapSceneExports(sceneId, videos);
        return new SceneExportListResponse(items, safePage, safeSize, totalCount, totalPages);
    }

    private List<SceneExportItemResponse> mapSceneExports(Long sceneId, List<SceneVideo> videos) {
        return videos.stream()
                .map(video -> {
                    String previewFallback = mediaUrlResolver.sceneExportPreviewUrl(sceneId, video.getId());
                    String downloadFallback = mediaUrlResolver.sceneExportFileUrl(sceneId, video.getId());
                    String previewUrl = assetUrlResolver.resolveUrl(video.getAssetId(), previewFallback);
                    String downloadUrl = assetUrlResolver.resolveDownloadUrl(
                            video.getAssetId(),
                            downloadFallback,
                            "scene-" + sceneId + ".mp4"
                    );
                    String thumbnailUrl = assetUrlResolver.resolveUrl(
                            video.getThumbnailAssetId(),
                            video.getThumbnailUrl()
                    );
                    return new SceneExportItemResponse(
                            video.getId(),
                            video.getSceneId(),
                            video.getAssetId(),
                            previewUrl,
                            downloadUrl,
                            thumbnailUrl,
                            video.getMergeSignature(),
                            video.getStatus(),
                            video.getDurationMs(),
                            Boolean.TRUE.equals(video.getIsActive()),
                            video.getCreatedAt(),
                            video.getUpdatedAt()
                    );
                })
                .toList();
    }

    @Transactional
    public void deleteExport(Long userId, Long sceneId, Long sceneVideoId) {
        Scene scene = sceneMapper.findById(sceneId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCENE_NOT_FOUND));
        projectAccessService.ensureProjectAccessible(scene.getProjectId(), userId);

        SceneVideo sceneVideo = sceneVideoMapper.findById(sceneVideoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXPORT_NOT_FOUND));
        if (!sceneId.equals(sceneVideo.getSceneId())) {
            throw new BusinessException(ErrorCode.EXPORT_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(sceneVideo.getIsActive())) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Active export cannot be deleted");
        }
        int deleted = sceneVideoMapper.deleteInactiveById(sceneVideoId, sceneId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.EXPORT_NOT_FOUND);
        }
    }
}
