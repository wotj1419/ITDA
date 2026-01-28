package com.itda.backend.object.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.asset.domain.Asset;
import com.itda.backend.asset.repository.AssetMapper;
import com.itda.backend.object.controller.dto.request.CreateObjectRequest;
import com.itda.backend.object.controller.dto.request.UpdateObjectRequest;
import com.itda.backend.object.controller.dto.response.ObjectSheetResponse;
import com.itda.backend.object.domain.ObjectSheet;
import com.itda.backend.object.domain.ObjectStatus;
import com.itda.backend.object.repository.ObjectMapper;
import com.itda.backend.object.storage.ObjectImageCleaner;
import com.itda.backend.object.storage.ObjectImageStorage;
import com.itda.backend.object.storage.ObjectImageStorageResult;
import com.itda.backend.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ObjectService {

    private final ObjectMapper objectMapper;
    private final ProjectAccessService projectAccessService;
    private final ObjectImageStorage objectImageStorage;
    private final ObjectAssetRegistrar objectAssetRegistrar;
    private final ObjectImageCleaner objectImageCleaner;
    private final AssetMapper assetMapper;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    @Transactional
    public ObjectSheetResponse createObject(Long userId, Long projectId, CreateObjectRequest request, MultipartFile file) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        validateImageFile(file);

        ObjectSheet objectSheet = ObjectSheet.create(
                projectId,
                request.name(),
                request.type(),
                request.description(),
                request.style(),
                userId
        );

        objectMapper.insertObject(objectSheet);
        if (objectSheet.getId() == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "object id missing");
        }

        ObjectImageStorageResult storedImage = storeImageOrThrow(projectId, objectSheet.getId(), file);
        Long assetId = null;
        try {
            assetId = objectAssetRegistrar.registerObjectImage(
                    userId,
                    projectId,
                    storedImage.storageKey(),
                    storedImage.sizeBytes(),
                    storedImage.contentType(),
                    storedImage.storageProvider()
            );
            updateObjectImage(objectSheet.getId(), assetId);
            return ObjectSheetResponse.from(requireObject(objectSheet.getId()));
        } catch (RuntimeException e) {
            cleanupStoredAsset(assetId, storedImage);
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<ObjectSheetResponse> listObjects(Long userId, Long projectId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);
        return objectMapper.findByProjectId(projectId).stream()
                .map(ObjectSheetResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ObjectSheetResponse getObject(Long userId, Long objectId) {
        ObjectSheet objectSheet = requireObject(objectId);
        projectAccessService.ensureProjectAccessible(objectSheet.getProjectId(), userId);
        return ObjectSheetResponse.from(objectSheet);
    }

    @Transactional
    public ObjectSheetResponse updateObject(Long userId, Long objectId, UpdateObjectRequest request) {
        validateUpdateRequest(request);

        ObjectSheet objectSheet = requireObject(objectId);
        projectAccessService.ensureProjectAccessible(objectSheet.getProjectId(), userId);

        int updated = objectMapper.updateObject(
                objectId,
                request.name(),
                request.type(),
                request.description(),
                request.style()
        );

        if (updated == 0) {
            throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND);
        }

        ObjectSheet updatedObject = requireObject(objectId);
        return ObjectSheetResponse.from(updatedObject);
    }

    @Transactional
    public void deleteObject(Long userId, Long objectId) {
        ObjectSheet objectSheet = requireObject(objectId);
        projectAccessService.ensureProjectAccessible(objectSheet.getProjectId(), userId);
        Asset asset = findAsset(objectSheet.getSheetImageAssetId());

        int deleted = objectMapper.deleteObject(objectId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND);
        }

        deleteAssetAndFile(asset);
    }

    @Transactional
    public ObjectSheetResponse replaceObjectImage(Long userId, Long objectId, MultipartFile file) {
        validateImageFile(file);

        ObjectSheet objectSheet = requireObject(objectId);
        projectAccessService.ensureProjectAccessible(objectSheet.getProjectId(), userId);
        Asset previousAsset = findAsset(objectSheet.getSheetImageAssetId());

        ObjectImageStorageResult storedImage = storeImageOrThrow(objectSheet.getProjectId(), objectId, file);
        Long assetId = null;
        try {
            assetId = objectAssetRegistrar.registerObjectImage(
                    userId,
                    objectSheet.getProjectId(),
                    storedImage.storageKey(),
                    storedImage.sizeBytes(),
                    storedImage.contentType(),
                    storedImage.storageProvider()
            );
            updateObjectImage(objectId, assetId);
        } catch (RuntimeException e) {
            cleanupStoredAsset(assetId, storedImage);
            throw e;
        }

        deleteAssetAndFile(previousAsset);
        return ObjectSheetResponse.from(requireObject(objectId));
    }

    private ObjectSheet requireObject(Long objectId) {
        return objectMapper.findById(objectId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OBJECT_NOT_FOUND));
    }

    private void validateUpdateRequest(UpdateObjectRequest request) {
        if (request.name() == null && request.type() == null
                && request.description() == null && request.style() == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "image file is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(normalizeContentType(contentType))) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "unsupported image content type");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && !filename.isBlank() && !hasAllowedExtension(filename)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "unsupported image file extension");
        }
    }

    private boolean hasAllowedExtension(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg")
                || lower.endsWith(".jpeg")
                || lower.endsWith(".png")
                || lower.endsWith(".webp");
    }

    private String normalizeContentType(String contentType) {
        String trimmed = contentType.trim().toLowerCase();
        int semicolon = trimmed.indexOf(';');
        return semicolon > 0 ? trimmed.substring(0, semicolon).trim() : trimmed;
    }

    private ObjectImageStorageResult storeImageOrThrow(Long projectId, Long objectId, MultipartFile file) {
        try {
            return objectImageStorage.save(projectId, objectId, file.getBytes(), file.getContentType());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, e);
        }
    }

    private void updateObjectImage(Long objectId, Long assetId) {
        String imageUrl = buildObjectImageUrl(objectId);
        int updated = objectMapper.updateSheetImage(objectId, imageUrl, assetId, ObjectStatus.SUCCEEDED);
        if (updated == 0) {
            throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND);
        }
    }

    private String buildObjectImageUrl(Long objectId) {
        if (objectId == null) {
            return null;
        }
        return "/api/objects/" + objectId + "/image";
    }

    private Asset findAsset(Long assetId) {
        if (assetId == null) {
            return null;
        }
        return assetMapper.findById(assetId).orElse(null);
    }

    private void deleteAssetAndFile(Asset asset) {
        if (asset == null) {
            return;
        }
        try {
            assetMapper.deleteById(asset.getId());
        } catch (Exception e) {
            log.warn("[ObjectService] Failed to delete asset record: assetId={}", asset.getId(), e);
        }
        objectImageCleaner.delete(asset.getStorageProvider(), asset.getStorageKey());
    }

    private void cleanupStoredAsset(Long assetId, ObjectImageStorageResult storedImage) {
        if (assetId != null) {
            try {
                assetMapper.deleteById(assetId);
            } catch (Exception e) {
                log.warn("[ObjectService] Failed to rollback asset record: assetId={}", assetId, e);
            }
        }
        if (storedImage != null) {
            objectImageCleaner.delete(storedImage.storageProvider(), storedImage.storageKey());
        }
    }
}
