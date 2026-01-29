package com.itda.backend.timeline.repository;

import com.itda.backend.timeline.domain.ProjectMerge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ProjectMergeMapper {

    int insert(ProjectMerge merge);

    int deactivateByProjectId(@Param("projectId") Long projectId);

    Optional<ProjectMerge> findActiveByProjectIdAndSignature(@Param("projectId") Long projectId,
                                                             @Param("mergeSignature") String mergeSignature);

    Optional<ProjectMerge> findActiveByProjectId(@Param("projectId") Long projectId);

    int updateAssetAndStatus(@Param("id") Long id,
                             @Param("assetId") Long assetId,
                             @Param("status") String status);
}
