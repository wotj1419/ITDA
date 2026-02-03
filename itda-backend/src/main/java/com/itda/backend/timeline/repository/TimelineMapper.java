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

    int countSceneTimelineItems(@Param("sceneId") Long sceneId);

    int countSceneTimelineItemsByVideoNodeIds(@Param("sceneId") Long sceneId,
                                              @Param("videoNodeIds") List<Long> videoNodeIds);

    int reorderSceneTimelineItems(@Param("sceneId") Long sceneId,
                                  @Param("orderedVideoNodeIds") List<Long> orderedVideoNodeIds);

    int countProjectTimelineItems(@Param("projectId") Long projectId);

    int countProjectTimelineItemsByVideoNodeIds(@Param("projectId") Long projectId,
                                                 @Param("videoNodeIds") List<Long> videoNodeIds);

    int reorderProjectTimelineItemsByVideoNodeIds(@Param("projectId") Long projectId,
                                                  @Param("orderedVideoNodeIds") List<Long> orderedVideoNodeIds);

    int insertSceneTimelineItem(@Param("projectId") Long projectId,
                                @Param("sceneId") Long sceneId,
                                @Param("videoNodeId") Long videoNodeId,
                                @Param("orderIndex") Integer orderIndex,
                                @Param("createdBy") Long createdBy);

    int deleteSceneTimelineItemByVideoNodeId(@Param("videoNodeId") Long videoNodeId);

    int deleteSceneTimelineItemsByShotId(@Param("shotNodeId") Long shotNodeId);

    int updateSceneTimelineItemsVideoId(@Param("sceneId") Long sceneId,
                                        @Param("sceneVideoId") Long sceneVideoId);
}
