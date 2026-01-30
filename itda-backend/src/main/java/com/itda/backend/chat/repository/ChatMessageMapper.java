package com.itda.backend.chat.repository;

import com.itda.backend.chat.domain.ChatMessage;
import com.itda.backend.chat.repository.dto.ChatMessageRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    void insertMessage(ChatMessage message);

    ChatMessageRow findById(@Param("messageId") Long messageId);

    List<ChatMessageRow> findMessages(@Param("projectId") Long projectId,
                                      @Param("beforeId") Long beforeId,
                                      @Param("limit") int limit);
}
