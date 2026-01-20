package com.itda.backend.project.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
