package com.itda.backend.scene.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SceneObjectMapper {

    int deleteBySceneId(@Param("sceneId") Long sceneId);

    int insertBatch(@Param("sceneId") Long sceneId,
                    @Param("objectIds") List<Long> objectIds);

    List<Long> findObjectIdsBySceneId(@Param("sceneId") Long sceneId);
}
