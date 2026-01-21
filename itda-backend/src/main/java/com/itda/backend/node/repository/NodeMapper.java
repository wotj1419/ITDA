package com.itda.backend.node.repository;

import com.itda.backend.node.controller.dto.request.NodePosition;
import com.itda.backend.node.domain.Node;
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
}
