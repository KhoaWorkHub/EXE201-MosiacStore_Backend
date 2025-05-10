package com.mosiacstore.mosiac.application.dto.chat;

import com.mosiacstore.mosiac.domain.chat.MessagePriority;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {
    private UUID roomId;
    private String content;
    private MessagePriority priority;
    private Boolean requiresAttention;
}