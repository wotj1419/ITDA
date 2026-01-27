package com.itda.backend.object.repository;

import com.itda.backend.object.domain.ObjectSheet;
import com.itda.backend.object.domain.ObjectType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ObjectMapper {

    void insertObject(ObjectSheet objectSheet);

    Optional<ObjectSheet> findById(@Param("id") Long id);

    List<ObjectSheet> findByProjectId(@Param("projectId") Long projectId);

    List<ObjectSheet> findByIds(@Param("ids") List<Long> ids);

    int countByProjectIdAndIds(@Param("projectId") Long projectId,
                               @Param("ids") List<Long> ids);

    int updateObject(@Param("id") Long id,
                     @Param("name") String name,
                     @Param("type") ObjectType type,
                     @Param("description") String description,
                     @Param("style") String style);

    int deleteObject(@Param("id") Long id);
}
