package com.itda.backend.object.service;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.object.controller.dto.request.CreateObjectRequest;
import com.itda.backend.object.controller.dto.request.UpdateObjectRequest;
import com.itda.backend.object.controller.dto.response.ObjectSheetResponse;
import com.itda.backend.object.domain.ObjectSheet;
import com.itda.backend.object.repository.ObjectMapper;
import com.itda.backend.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ObjectService {

    private final ObjectMapper objectMapper;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public ObjectSheetResponse createObject(Long userId, Long projectId, CreateObjectRequest request) {
        projectAccessService.ensureProjectAccessible(projectId, userId);

        ObjectSheet objectSheet = ObjectSheet.create(
                projectId,
                request.name(),
                request.type(),
                request.description(),
                request.style(),
                userId
        );

        objectMapper.insertObject(objectSheet);
        return ObjectSheetResponse.from(objectSheet);
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

        int deleted = objectMapper.deleteObject(objectId);
        if (deleted == 0) {
            throw new BusinessException(ErrorCode.OBJECT_NOT_FOUND);
        }
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
}
