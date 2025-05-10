package com.mosiacstore.mosiac.application.mapper;

import com.mosiacstore.mosiac.application.dto.chat.*;
import com.mosiacstore.mosiac.domain.chat.ChatMessage;
import com.mosiacstore.mosiac.domain.chat.ChatRoom;
import com.mosiacstore.mosiac.domain.user.User;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ChatMapper {

    @Mapping(target = "participants", source = "participants")
    @Mapping(target = "lastMessage", source = "messages", qualifiedByName = "findLastMessage")
    @Mapping(target = "unreadCount", ignore = true)
    ChatRoomResponse toChatRoomResponse(ChatRoom room);

    @Named("findLastMessage")
    default ChatMessageResponse findLastMessage(java.util.Set<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        // Find the message with the most recent timestamp
        ChatMessage lastMessage = messages.stream()
                .sorted((m1, m2) -> m2.getCreatedAt().compareTo(m1.getCreatedAt()))
                .findFirst()
                .orElse(null);

        return lastMessage == null ? null : toChatMessageResponse(lastMessage);
    }

    @Mapping(target = "id", source = "participant.id")
    @Mapping(target = "email", source = "participant.email")
    @Mapping(target = "fullName", source = "participant.fullName")
    @Mapping(target = "role", source = "participant.role")
    @Mapping(target = "isOnline", ignore = true)
    ChatParticipantResponse toChatParticipantResponse(User participant);

    default List<ChatParticipantResponse> toChatParticipantResponseList(java.util.Set<User> participants) {
        if (participants == null) {
            return java.util.Collections.emptyList();
        }

        return participants.stream()
                .map(this::toChatParticipantResponse)
                .collect(Collectors.toList());
    }

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "senderName", source = "sender.fullName")
    @Mapping(target = "attachments", source = "attachments")
    ChatMessageResponse toChatMessageResponse(ChatMessage message);

    @Mapping(target = "id", source = "attachment.id")
    @Mapping(target = "messageId", source = "attachment.message.id")
    ChatAttachmentResponse toChatAttachmentResponse(com.mosiacstore.mosiac.domain.chat.ChatAttachment attachment);

    default List<ChatAttachmentResponse> toChatAttachmentResponseList(java.util.Set<com.mosiacstore.mosiac.domain.chat.ChatAttachment> attachments) {
        if (attachments == null) {
            return java.util.Collections.emptyList();
        }

        return attachments.stream()
                .map(this::toChatAttachmentResponse)
                .collect(Collectors.toList());
    }
}