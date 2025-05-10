package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.chat.ChatMessageRequest;
import com.mosiacstore.mosiac.application.dto.chat.ChatMessageResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.domain.chat.MessagePriority;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ChatMessageService {

    ChatMessageResponse sendMessage(ChatMessageRequest request, UUID senderId);

    ChatMessageResponse sendSystemMessage(UUID roomId, String content);

    List<ChatMessageResponse> getRoomMessages(UUID roomId, UUID userId);

    ChatMessageResponse getMessageById(UUID messageId);

    PageResponse<ChatMessageResponse> getPaginatedMessages(UUID roomId, int page, int size);

    List<ChatMessageResponse> getMessagesBefore(UUID roomId, LocalDateTime timestamp, int limit);

    List<ChatMessageResponse> getMessagesAfter(UUID roomId, LocalDateTime timestamp, int limit);

    void markAsRead(UUID messageId, UUID userId);

    void markAllAsReadInRoom(UUID roomId, UUID userId);

    ChatMessageResponse updateMessagePriority(UUID messageId, MessagePriority priority, UUID staffId);

    ChatMessageResponse flagForAttention(UUID messageId, Boolean requiresAttention, UUID staffId);

    ChatMessageResponse updateMessageContent(UUID messageId, String newContent, UUID editorId);

    void deleteMessage(UUID messageId, UUID userId);

    PageResponse<ChatMessageResponse> getMessagesRequiringAttention(int page, int size);

    PageResponse<ChatMessageResponse> getMessagesByPriority(MessagePriority priority, int page, int size);

    long countPendingCustomerMessages();

    void updateMessageStatus(UUID messageId, UUID userId, boolean delivered, boolean read);

    ChatMessageResponse sendMessageWithAttachment(ChatMessageRequest request, MultipartFile file, UUID senderId);
}