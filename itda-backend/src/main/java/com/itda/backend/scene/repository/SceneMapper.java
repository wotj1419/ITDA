package com.itda.backend.scene.repository;

import com.itda.backend.scene.domain.Scene;
import com.itda.backend.scene.repository.dto.SceneSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface SceneMapper {

    void insertScene(Scene scene);

    Optional<Scene> findById(@Param("id") Long id);

    Optional<Scene> findByIdForUpdate(@Param("id") Long id);

    List<SceneSummary> findAllByProjectId(@Param("projectId") Long projectId);

    int findMaxOrderIndex(@Param("projectId") Long projectId);
}
