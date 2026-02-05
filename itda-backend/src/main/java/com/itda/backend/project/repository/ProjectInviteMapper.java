package com.itda.backend.project.repository;

import com.itda.backend.project.controller.dto.response.ProjectInviteResponse;
import com.itda.backend.project.domain.ProjectInvite;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ProjectInviteMapper {

    void insertInvite(ProjectInvite invite);

    Optional<ProjectInvite> findById(@Param("inviteId") Long inviteId);

    Optional<ProjectInvite> findByProjectIdAndReceiverId(@Param("projectId") Long projectId,
                                                         @Param("receiverId") Long receiverId);

    int updateInviteForResend(@Param("inviteId") Long inviteId,
                              @Param("senderId") Long senderId,
                              @Param("role") String role);

    int updateInviteStatus(@Param("inviteId") Long inviteId,
                           @Param("status") String status);

    List<ProjectInviteResponse> findAllByReceiverId(@Param("receiverId") Long receiverId);

    Optional<ProjectInviteResponse> findResponseById(@Param("inviteId") Long inviteId);
}
