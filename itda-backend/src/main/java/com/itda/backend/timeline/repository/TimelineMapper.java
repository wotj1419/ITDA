package com.itda.backend.timeline.repository;

import com.itda.backend.timeline.repository.dto.ProjectTimelineItem;
import com.itda.backend.timeline.repository.dto.SceneTimelineItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface TimelineMapper {

    List<SceneTimelineItem> findSceneTimelineItems(@Param("sceneId") Long sceneId);

    List<ProjectTimelineItem> findProjectTimelineItems(@Param("projectId") Long projectId);

    int insertSceneTimelineItem(@Param("projectId") Long projectId,
                                @Param("sceneId") Long sceneId,
                                @Param("videoNodeId") Long videoNodeId,
                                @Param("orderIndex") Integer orderIndex,
                                @Param("createdBy") Long createdBy);

    int deleteSceneTimelineItemByVideoNodeId(@Param("videoNodeId") Long videoNodeId);

    int deleteSceneTimelineItemsByShotId(@Param("shotNodeId") Long shotNodeId);
}
