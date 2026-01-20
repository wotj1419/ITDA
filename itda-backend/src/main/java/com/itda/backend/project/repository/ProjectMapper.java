package com.itda.backend.project.repository;

import com.itda.backend.project.domain.Project;
import com.itda.backend.project.repository.dto.ProjectSummary;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProjectMapper {

    void insertProject(Project project);

    Optional<Project> findById(@Param("id") Long id);

    List<ProjectSummary> findAllByUserId(@Param("userId") Long userId);
}
