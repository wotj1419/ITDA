package com.itda.backend.project.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.itda.backend.project.controller.dto.response.ProjectMemberResponse;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProjectMemberMapper {

    void insertMember(@Param("projectId") Long projectId,
                      @Param("userId") Long userId,
                      @Param("role") String role);

    boolean existsMember(@Param("projectId") Long projectId,
                         @Param("userId") Long userId);

    Optional<String> findRole(@Param("projectId") Long projectId,
                              @Param("userId") Long userId);

    int countByProjectId(@Param("projectId") Long projectId);

    List<ProjectMemberResponse> findAllMembers(@Param("projectId") Long projectId);

    int updateMemberRole(@Param("projectId") Long projectId,
                         @Param("userId") Long userId,
                         @Param("role") String role);

    int deleteMember(@Param("projectId") Long projectId,
                     @Param("userId") Long userId);
}
