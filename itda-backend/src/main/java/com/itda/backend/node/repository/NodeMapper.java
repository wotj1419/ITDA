package com.itda.backend.node.repository;

import com.itda.backend.node.controller.dto.request.NodePosition;
import com.itda.backend.node.domain.Node;
import com.itda.backend.node.domain.NodeStatus;
import com.itda.backend.node.repository.dto.TimelineNodeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * Node MyBatis Mapper 인터페이스
 */
@Mapper
public interface NodeMapper {

    void insertNode(Node node);

    Optional<Node> findById(@Param("id") Long id);

    List<Node> findAllBySceneId(@Param("sceneId") Long sceneId);

    void updateNode(Node node);

    void deleteById(@Param("id") Long id);

    int countMasterNodesBySceneId(@Param("sceneId") Long sceneId);

    void updatePositions(@Param("sceneId") Long sceneId,
                         @Param("positions") List<NodePosition> positions);

    int findMaxOrderIndex(@Param("sceneId") Long sceneId);

    int clearActiveMasterBySceneId(@Param("sceneId") Long sceneId);

    int setActiveMaster(@Param("nodeId") Long nodeId);

    int clearConfirmedByShotId(@Param("shotNodeId") Long shotNodeId);

    int setConfirmedVideo(@Param("nodeId") Long nodeId);

    int clearConfirmedVideo(@Param("nodeId") Long nodeId);

    int updateStatus(@Param("nodeId") Long nodeId,
                     @Param("status") NodeStatus status);

    int updateStatusAndContentUrl(@Param("nodeId") Long nodeId,
                                  @Param("status") NodeStatus status,
                                  @Param("contentUrl") String contentUrl);

    List<TimelineNodeRow> findConfirmedVideoNodesByProjectId(@Param("projectId") Long projectId);

    List<TimelineNodeRow> findConfirmedVideoNodesBySceneId(@Param("sceneId") Long sceneId);
}
