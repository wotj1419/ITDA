package com.itda.backend.timeline.repository;

import com.itda.backend.timeline.domain.SceneVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface SceneVideoMapper {

    int insert(SceneVideo video);

    int deactivateBySceneId(@Param("sceneId") Long sceneId);

    Optional<SceneVideo> findActiveBySceneIdAndSignature(@Param("sceneId") Long sceneId,
                                                        @Param("mergeSignature") String mergeSignature);

    Optional<SceneVideo> findActiveBySceneId(@Param("sceneId") Long sceneId);

    int updateAssetAndStatus(@Param("id") Long id,
                             @Param("assetId") Long assetId,
                             @Param("status") String status,
                             @Param("thumbnailUrl") String thumbnailUrl,
                             @Param("durationMs") Integer durationMs);
}
