package com.mosiacstore.mosiac.application.dto.chat;

import com.mosiacstore.mosiac.domain.chat.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomRequest {
    private String name;
    private ChatRoomType type;
    private List<UUID> participantIds;
}