package com.itda.backend.auth.controller;

import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.domain.AssetType;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.asset.service.AssetUrlResolver;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@Tag(name = "프로필 이미지", description = "프로필 이미지 조회 API")
@RestController
@RequestMapping("/api/profile-images")
@RequiredArgsConstructor
public class ProfileImageController {

    private final AssetMapper assetMapper;
    private final AssetUrlResolver assetUrlResolver;

    @Operation(summary = "프로필 이미지 조회", description = "프로필 이미지 assetId로 접근합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "리다이렉트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "이미지 없음")
    })
    @GetMapping("/{assetId}")
    public ResponseEntity<Void> getProfileImage(@PathVariable Long assetId) {
        Asset asset = assetMapper.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONTENT_NOT_FOUND));
        if (asset.getProjectId() != null || asset.getAssetType() != AssetType.IMAGE) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        String url = assetUrlResolver.resolvePublicUrl(assetId, null);
        if (url == null || url.isBlank()) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_FOUND);
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(url))
                .build();
    }
}
