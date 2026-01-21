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

    int countByUserId(@Param("userId") Long userId);

    List<ProjectSummary> findAllByUserId(@Param("userId") Long userId,
                                         @Param("limit") int limit,
                                         @Param("offset") int offset);

    int updateProject(@Param("id") Long id,
                      @Param("title") String title,
                      @Param("description") String description,
                      @Param("genre") String genre);

    int deleteProject(@Param("id") Long id);
}
