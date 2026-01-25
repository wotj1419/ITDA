package com.itda.backend.asset.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Asset 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    private Long id;
    private Long ownerId;
    private Long projectId;
    private AssetType assetType;
    private String storageProvider;
    private String storageKey;
    private String contentType;
    private Long sizeBytes;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
}
