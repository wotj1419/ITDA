package com.itda.backend.timeline.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Merge request")
public record MergeRequest(

        @Schema(description = "Include music in merge result (P1)", example = "false")
        Boolean includeMusic
) {

    public boolean includeMusicOrFalse() {
        return Boolean.TRUE.equals(includeMusic);
    }
}
