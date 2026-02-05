package com.itda.backend.project.repository.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProjectPreviewCandidate {

    private Long videoAssetId;
    private String videoFallbackUrl;
    private Long thumbnailAssetId;
    private String thumbnailFallbackUrl;
}
