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
}
