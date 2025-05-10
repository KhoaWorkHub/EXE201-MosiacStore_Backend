package com.mosiacstore.mosiac.application.dto.chat;

import com.mosiacstore.mosiac.domain.chat.MessagePriority;
import com.mosiacstore.mosiac.domain.chat.MessageStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {
    private UUID id;
    private UUID roomId;
    private UUID senderId;
    private String senderName;
    private String content;
    private MessageStatus status;
    private MessagePriority priority;
    private Boolean requiresAttention;
    private Boolean isSystemMessage;
    private List<ChatAttachmentResponse> attachments;
    private LocalDateTime createdAt;
}