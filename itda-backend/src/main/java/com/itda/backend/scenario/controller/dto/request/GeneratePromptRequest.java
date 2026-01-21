package com.itda.backend.scenario.controller.dto.request;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "Scenario prompt generation request")
public record GeneratePromptRequest(

        @Schema(description = "Genre", example = "SF")
        @NotBlank(message = "genre is required")
        @Size(max = 50, message = "genre must be 50 characters or less")
        String genre,

        @Schema(description = "Mood", example = "HOPEFUL")
        @NotBlank(message = "mood is required")
        @Size(max = 50, message = "mood must be 50 characters or less")
        String mood,

        @Schema(description = "Scene count", example = "5")
        @NotNull(message = "sceneCount is required")
        @Min(value = 3, message = "sceneCount must be at least 3")
        @Max(value = 7, message = "sceneCount must be 7 or less")
        Integer sceneCount,

        @Schema(description = "Keywords (comma-separated string or array)", example = "화성, 생존, 가족")
        @JsonDeserialize(using = KeywordsDeserializer.class)
        List<String> keywords,

        @Schema(description = "Character hints", example = "외로운 우주인")
        @Size(max = 2000, message = "characterHints must be 2000 characters or less")
        String characterHints,

        @Schema(description = "Background hints", example = "화성 기지")
        @Size(max = 2000, message = "backgroundHints must be 2000 characters or less")
        String backgroundHints,

        @Schema(description = "Reference style", example = "인터스텔라")
        @Size(max = 100, message = "referenceStyle must be 100 characters or less")
        String referenceStyle
) {
}
