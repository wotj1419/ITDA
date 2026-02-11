package com.itda.backend.timeline.repository;

import com.itda.backend.timeline.domain.SceneVideo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SceneVideoMapper {

    int insert(SceneVideo video);

    int deactivateBySceneId(@Param("sceneId") Long sceneId);

    Optional<SceneVideo> findById(@Param("sceneVideoId") Long sceneVideoId);

    List<SceneVideo> findBySceneId(@Param("sceneId") Long sceneId);

    int countBySceneId(@Param("sceneId") Long sceneId);

    List<SceneVideo> findBySceneIdPaged(@Param("sceneId") Long sceneId,
                                        @Param("limit") int limit,
                                        @Param("offset") int offset);

    int activateBySceneId(@Param("sceneId") Long sceneId,
                          @Param("sceneVideoId") Long sceneVideoId);

    Optional<SceneVideo> findActiveBySceneIdAndSignature(@Param("sceneId") Long sceneId,
                                                        @Param("mergeSignature") String mergeSignature);

    Optional<SceneVideo> findLatestBySceneIdAndSignature(@Param("sceneId") Long sceneId,
                                                          @Param("mergeSignature") String mergeSignature);

    Optional<SceneVideo> findActiveBySceneId(@Param("sceneId") Long sceneId);

    int deleteInactiveById(@Param("sceneVideoId") Long sceneVideoId,
                           @Param("sceneId") Long sceneId);

    int updateAssetAndStatus(@Param("id") Long id,
                             @Param("assetId") Long assetId,
                             @Param("thumbnailAssetId") Long thumbnailAssetId,
                             @Param("status") String status,
                             @Param("thumbnailUrl") String thumbnailUrl,
                             @Param("durationMs") Integer durationMs);
}
