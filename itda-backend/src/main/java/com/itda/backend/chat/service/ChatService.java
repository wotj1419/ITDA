package com.itda.backend.chat.service;

import com.itda.backend.chat.controller.dto.ChatMessagePage;
import com.itda.backend.chat.controller.dto.ChatMessageResponse;
import com.itda.backend.chat.controller.dto.ChatMessageSender;
import com.itda.backend.chat.controller.dto.ChatSendRequest;
import com.itda.backend.chat.domain.ChatMessage;
import com.itda.backend.chat.repository.ChatMessageMapper;
import com.itda.backend.chat.repository.dto.ChatMessageRow;
import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import com.itda.backend.project.service.ProjectAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_CONTENT_LENGTH = 2000;
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_TYPE = "TEXT";

    private final ChatMessageMapper chatMessageMapper;
    private final ProjectAccessService projectAccessService;

    @Transactional
    public ChatMessageResponse createMessage(Long projectId, Long senderId, ChatSendRequest request) {
        projectAccessService.ensureProjectAccessible(projectId, senderId);

        String content = requireContent(request.content());
        String type = normalizeType(request.type());

        ChatMessage message = ChatMessage.builder()
                .projectId(projectId)
                .senderId(senderId)
                .content(content)
                .type(type)
                .build();
        chatMessageMapper.insertMessage(message);

        ChatMessageRow row = chatMessageMapper.findById(message.getId());
        if (row == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
        return toResponse(row);
    }

    @Transactional(readOnly = true)
    public ChatMessagePage getMessages(Long projectId, Long userId, Integer size, Long beforeId) {
        projectAccessService.ensureProjectAccessible(projectId, userId);

        int resolvedSize = resolveSize(size);
        Long resolvedBefore = resolveBefore(beforeId);

        List<ChatMessageRow> rows = chatMessageMapper.findMessages(projectId, resolvedBefore, resolvedSize + 1);
        boolean hasMore = rows.size() > resolvedSize;
        if (hasMore) {
            rows = rows.subList(0, resolvedSize);
        }

        List<ChatMessageResponse> items = rows.stream()
                .map(this::toResponse)
                .toList();
        return new ChatMessagePage(items, hasMore);
    }

    private String requireContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return content;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return DEFAULT_TYPE;
        }
        String normalized = type.trim().toUpperCase();
        if (!DEFAULT_TYPE.equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private int resolveSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return size;
    }

    private Long resolveBefore(Long beforeId) {
        if (beforeId == null) {
            return null;
        }
        if (beforeId < 1) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        return beforeId;
    }

    private ChatMessageResponse toResponse(ChatMessageRow row) {
        ChatMessageSender sender = new ChatMessageSender(
                row.getSenderId(),
                row.getSenderName(),
                row.getSenderProfileImageUrl()
        );
        return new ChatMessageResponse(
                row.getMessageId(),
                row.getProjectId(),
                sender,
                row.getContent(),
                row.getType(),
                row.getCreatedAt()
        );
    }
}
