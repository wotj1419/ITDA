package com.itda.backend.chat.repository.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRow {

    private Long messageId;
    private Long projectId;
    private Long senderId;
    private String senderName;
    private String senderProfileImageUrl;
    private String content;
    private String type;
    private LocalDateTime createdAt;
}
