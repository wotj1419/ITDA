package com.itda.backend.scenario.repository;

import com.itda.backend.scenario.repository.dto.ScenarioRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface ScenarioMapper {

    Optional<ScenarioRecord> findByProjectId(@Param("projectId") Long projectId);

    void insertScenario(ScenarioRecord record);

    int updateInputAndPrompt(ScenarioRecord record);

    int updatePrompt(@Param("projectId") Long projectId,
                     @Param("promptText") String promptText,
                     @Param("promptStatus") String promptStatus,
                     @Param("currentStep") String currentStep,
                     @Param("plotText") String plotText,
                     @Param("plotStatus") String plotStatus,
                     @Param("version") Integer version);

    int updatePlot(@Param("projectId") Long projectId,
                   @Param("plotText") String plotText,
                   @Param("plotStatus") String plotStatus,
                   @Param("currentStep") String currentStep,
                   @Param("version") Integer version);

    int updateCurrentStep(@Param("projectId") Long projectId,
                          @Param("currentStep") String currentStep,
                          @Param("version") Integer version);
}
