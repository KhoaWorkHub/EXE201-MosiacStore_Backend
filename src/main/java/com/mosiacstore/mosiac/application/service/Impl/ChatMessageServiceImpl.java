package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.chat.ChatAttachmentResponse;
import com.mosiacstore.mosiac.application.dto.chat.ChatMessageRequest;
import com.mosiacstore.mosiac.application.dto.chat.ChatMessageResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.mapper.ChatMapper;
import com.mosiacstore.mosiac.application.service.ChatMessageService;
import com.mosiacstore.mosiac.application.service.NotificationService;
import com.mosiacstore.mosiac.domain.chat.*;
import com.mosiacstore.mosiac.domain.notification.NotificationType;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.infrastructure.repository.*;
import com.mosiacstore.mosiac.infrastructure.service.MinioService;
import com.mosiacstore.mosiac.infrastructure.service.StorageServiceDelegate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final ChatAttachmentRepository attachmentRepository;
    private final StorageServiceDelegate storageServiceDelegate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMapper chatMapper;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public ChatMessageResponse sendMessage(ChatMessageRequest request, UUID senderId) {
        ChatRoom room = chatRoomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + request.getRoomId()));

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + senderId));

        // Validate that sender is a participant
        boolean isParticipant = room.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(senderId));

        if (!isParticipant) {
            throw new InvalidOperationException("User is not a participant in this chat room");
        }

        // Create and save message
        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(sender);
        message.setContent(request.getContent());
        message.setStatus(MessageStatus.SENT);
        message.setPriority(request.getPriority() != null ? request.getPriority() : MessagePriority.NORMAL);
        message.setRequiresAttention(request.getRequiresAttention() != null ? request.getRequiresAttention() : false);
        message.setIsSystemMessage(false);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Create read status for each participant
        for (User participant : room.getParticipants()) {
            MessageReadStatus readStatus = new MessageReadStatus();
            readStatus.setMessage(savedMessage);
            readStatus.setUser(participant);
            readStatus.setIsRead(participant.getId().equals(senderId)); // Mark as read for sender
            readStatus.setCreatedAt(LocalDateTime.now());
            readStatus.setUpdatedAt(LocalDateTime.now());
            readStatusRepository.save(readStatus);
        }

        // Update room's last activity
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        // Convert to response DTO
        ChatMessageResponse response = chatMapper.toChatMessageResponse(savedMessage);

        // Send message to websocket topic
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), response);

        // Send notifications to other participants
        for (User participant : room.getParticipants()) {
            if (!participant.getId().equals(senderId)) {
                // Create message preview (limit to 50 chars)
                String preview = request.getContent().length() > 50
                        ? request.getContent().substring(0, 47) + "..."
                        : request.getContent();

                // Send notification
                notificationService.sendChatNotification(
                        participant.getId(),
                        room.getId(),
                        savedMessage.getId(),
                        sender.getFullName(),
                        preview
                );
            }
        }

        return response;
    }

    @Override
    @Transactional
    public ChatMessageResponse sendSystemMessage(UUID roomId, String content) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + roomId));

        // Create and save system message
        ChatMessage message = new ChatMessage();
        message.setRoom(room);
        message.setSender(null); // No sender for system messages
        message.setContent(content);
        message.setStatus(MessageStatus.DELIVERED);
        message.setPriority(MessagePriority.NORMAL);
        message.setRequiresAttention(false);
        message.setIsSystemMessage(true);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());

        ChatMessage savedMessage = chatMessageRepository.save(message);

        // Create read status for each participant
        for (User participant : room.getParticipants()) {
            MessageReadStatus readStatus = new MessageReadStatus();
            readStatus.setMessage(savedMessage);
            readStatus.setUser(participant);
            readStatus.setIsRead(false);
            readStatus.setCreatedAt(LocalDateTime.now());
            readStatus.setUpdatedAt(LocalDateTime.now());
            readStatusRepository.save(readStatus);
        }

        // Update room's last activity
        room.setUpdatedAt(LocalDateTime.now());
        chatRoomRepository.save(room);

        // Convert to response DTO
        ChatMessageResponse response = chatMapper.toChatMessageResponse(savedMessage);

        // Send message to websocket topic
        messagingTemplate.convertAndSend("/topic/room/" + room.getId(), response);

        return response;
    }

    @Override
    public List<ChatMessageResponse> getRoomMessages(UUID roomId, UUID userId) {
        // Verify room exists and user is participant
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + roomId));

        boolean isParticipant = room.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(userId));

        if (!isParticipant) {
            throw new InvalidOperationException("User is not a participant in this chat room");
        }

        List<ChatMessage> messages = chatMessageRepository.findByRoomIdOrderByCreatedAtAsc(roomId);
        return messages.stream()
                .map(chatMapper::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ChatMessageResponse getMessageById(UUID messageId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));

        return chatMapper.toChatMessageResponse(message);
    }

    @Override
    public PageResponse<ChatMessageResponse> getPaginatedMessages(UUID roomId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository.findByRoomId(roomId, pageable);

        List<ChatMessageResponse> messageResponses = messagePage.getContent().stream()
                .map(chatMapper::toChatMessageResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                messageResponses,
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.isFirst(),
                messagePage.isLast()
        );
    }

    @Override
    public List<ChatMessageResponse> getMessagesBefore(UUID roomId, LocalDateTime timestamp, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<ChatMessage> messages = chatMessageRepository.findMessagesBefore(roomId, timestamp, pageable);

        return messages.stream()
                .map(chatMapper::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageResponse> getMessagesAfter(UUID roomId, LocalDateTime timestamp, int limit) {
        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.ASC, "createdAt"));
        List<ChatMessage> messages = chatMessageRepository.findMessagesAfter(roomId, timestamp, pageable);

        return messages.stream()
                .map(chatMapper::toChatMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsRead(UUID messageId, UUID userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));

        // Find read status
        MessageReadStatus readStatus = readStatusRepository.findByMessageIdAndUserId(messageId, userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

                    MessageReadStatus newStatus = new MessageReadStatus();
                    newStatus.setMessage(message);
                    newStatus.setUser(user);
                    newStatus.setIsRead(false);
                    newStatus.setCreatedAt(LocalDateTime.now());
                    newStatus.setUpdatedAt(LocalDateTime.now());
                    return readStatusRepository.save(newStatus);
                });

        // Mark as read
        readStatus.setIsRead(true);
        readStatus.setUpdatedAt(LocalDateTime.now());
        readStatusRepository.save(readStatus);

        // Notify sender
        if (message.getSender() != null && !message.getSender().getId().equals(userId)) {
            messagingTemplate.convertAndSendToUser(
                    message.getSender().getEmail(),
                    "/queue/message-status",
                    Map.of(
                            "messageId", message.getId().toString(),
                            "status", "READ",
                            "userId", userId.toString()
                    )
            );
        }
    }

    @Override
    @Transactional
    public void markAllAsReadInRoom(UUID roomId, UUID userId) {
        readStatusRepository.markAllAsReadInRoom(roomId, userId);
    }

    @Override
    @Transactional
    public ChatMessageResponse updateMessagePriority(UUID messageId, MessagePriority priority, UUID staffId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + staffId));

        // Verify staff permissions
        if (!staff.getRole().name().equals("ADMIN") && !staff.getRole().name().equals("STAFF")) {
            throw new InvalidOperationException("User does not have permission to update message priority");
        }

        message.setPriority(priority);
        message.setUpdatedAt(LocalDateTime.now());
        ChatMessage updatedMessage = chatMessageRepository.save(message);

        // Notify participants
        ChatMessageResponse response = chatMapper.toChatMessageResponse(updatedMessage);
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoom().getId() + "/updates", response);

        return response;
    }

    @Override
    @Transactional
    public ChatMessageResponse flagForAttention(UUID messageId, Boolean requiresAttention, UUID staffId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));

        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + staffId));

        // Verify staff permissions
        if (!staff.getRole().name().equals("ADMIN") && !staff.getRole().name().equals("STAFF")) {
            throw new InvalidOperationException("User does not have permission to flag messages");
        }

        message.setRequiresAttention(requiresAttention);
        message.setUpdatedAt(LocalDateTime.now());
        ChatMessage updatedMessage = chatMessageRepository.save(message);

        // Notify participants
        ChatMessageResponse response = chatMapper.toChatMessageResponse(updatedMessage);
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoom().getId() + "/updates", response);

        // Notify staff if flagged for attention
        if (Boolean.TRUE.equals(requiresAttention)) {
            // Create notification for all staff/admin
            List<User> staffUsers = userRepository.findAll().stream()
                    .filter(user -> user.getRole().name().equals("ADMIN") || user.getRole().name().equals("STAFF"))
                    .collect(Collectors.toList());

            for (User staffUser : staffUsers) {
                if (!staffUser.getId().equals(staffId)) {
                    notificationService.createNotification(
                            staffUser.getId(),
                            "Message flagged for attention",
                            "A message in room " + message.getRoom().getName() + " requires attention",
                            NotificationType.SUPPORT_REQUEST,
                            "MESSAGE",
                            message.getId().toString(),
                            "/admin/chat/rooms/" + message.getRoom().getId()
                    );
                }
            }
        }

        return response;
    }

    @Override
    @Transactional
    public ChatMessageResponse updateMessageContent(UUID messageId, String newContent, UUID editorId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));

        // Only the sender can edit their own message
        if (!message.getSender().getId().equals(editorId)) {
            throw new InvalidOperationException("Only the sender can edit their message");
        }

        // Messages can only be edited within 24 hours
        if (message.getCreatedAt().plusHours(24).isBefore(LocalDateTime.now())) {
            throw new InvalidOperationException("Messages can only be edited within 24 hours of sending");
        }

        message.setContent(newContent);
        message.setUpdatedAt(LocalDateTime.now());
        ChatMessage updatedMessage = chatMessageRepository.save(message);

        // Notify participants
        ChatMessageResponse response = chatMapper.toChatMessageResponse(updatedMessage);
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoom().getId() + "/updates", response);

        return response;
    }

    @Override
    @Transactional
    public void deleteMessage(UUID messageId, UUID userId) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        // Check permissions
        boolean canDelete = message.getSender().getId().equals(userId) ||
                user.getRole().name().equals("ADMIN") ||
                user.getRole().name().equals("STAFF");

        if (!canDelete) {
            throw new InvalidOperationException("User does not have permission to delete this message");
        }

        // Delete attachments first
        for (ChatAttachment attachment : message.getAttachments()) {
            try {
                storageServiceDelegate.deleteFile(attachment.getFileUrl());
                if (attachment.getThumbnailUrl() != null) {
                    storageServiceDelegate.deleteFile(attachment.getThumbnailUrl());
                }
            } catch (Exception e) {
                log.warn("Failed to delete attachment file: {}", attachment.getFileUrl(), e);
            }
        }

        UUID roomId = message.getRoom().getId();

        // Delete the message
        chatMessageRepository.delete(message);

        // Notify participants
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/deleted", Map.of("messageId", messageId.toString()));
    }

    @Override
    public PageResponse<ChatMessageResponse> getMessagesRequiringAttention(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository.findMessagesRequiringAttention(pageable);

        List<ChatMessageResponse> messageResponses = messagePage.getContent().stream()
                .map(chatMapper::toChatMessageResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                messageResponses,
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.isFirst(),
                messagePage.isLast()
        );
    }

    @Override
    public PageResponse<ChatMessageResponse> getMessagesByPriority(MessagePriority priority, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<ChatMessage> messagePage = chatMessageRepository.findByPriority(priority, pageable);

        List<ChatMessageResponse> messageResponses = messagePage.getContent().stream()
                .map(chatMapper::toChatMessageResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                messageResponses,
                messagePage.getNumber(),
                messagePage.getSize(),
                messagePage.getTotalElements(),
                messagePage.getTotalPages(),
                messagePage.isFirst(),
                messagePage.isLast()
        );
    }

    @Override
    public long countPendingCustomerMessages() {
        return chatMessageRepository.countPendingCustomerMessages();
    }

    @Override
    @Transactional
    public void updateMessageStatus(UUID messageId, UUID userId, boolean delivered, boolean read) {
        ChatMessage message = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageId));

        // Update read status if needed
        if (read) {
            markAsRead(messageId, userId);
        }

        // Update message status
        if (delivered && message.getStatus() == MessageStatus.SENT) {
            message.setStatus(MessageStatus.DELIVERED);
            chatMessageRepository.save(message);
        }

        if (read && (message.getStatus() == MessageStatus.SENT || message.getStatus() == MessageStatus.DELIVERED)) {
            message.setStatus(MessageStatus.READ);
            chatMessageRepository.save(message);
        }
    }

    @Override
    @Transactional
    public ChatMessageResponse sendMessageWithAttachment(ChatMessageRequest request, MultipartFile file, UUID senderId) {
        // First create and save the message
        ChatMessageResponse messageResponse = sendMessage(request, senderId);

        // Get the saved message
        ChatMessage message = chatMessageRepository.findById(UUID.fromString(messageResponse.getId().toString()))
                .orElseThrow(() -> new EntityNotFoundException("Message not found with ID: " + messageResponse.getId()));

        // Upload file to MinIO
        String fileUrl;
        try {
            fileUrl = storageServiceDelegate.uploadFile(file, "chat");
        } catch (Exception e) {
            log.error("Failed to upload file for chat message", e);
            throw new RuntimeException("Failed to upload attachment: " + e.getMessage());
        }

        // Create attachment
        ChatAttachment attachment = new ChatAttachment();
        attachment.setMessage(message);
        attachment.setFileUrl(fileUrl);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());

        // Check if it's an image
        boolean isImage = file.getContentType() != null && file.getContentType().startsWith("image/");
        attachment.setIsImage(isImage);

        // If it's an image, create a thumbnail (optional, implementation depends on your needs)
        if (isImage) {
            // For simplicity, we'll use the original as thumbnail
            attachment.setThumbnailUrl(fileUrl);
        }

        // Save attachment
        ChatAttachment savedAttachment = attachmentRepository.save(attachment);

        // Update message with attachment
        if (message.getAttachments() == null) {
            message.setAttachments(new HashSet<>());
        }
        message.getAttachments().add(savedAttachment);
        chatMessageRepository.save(message);

        // Create attachment response
        ChatAttachmentResponse attachmentResponse = ChatAttachmentResponse.builder()
                .id(savedAttachment.getId())
                .messageId(message.getId())
                .fileUrl(savedAttachment.getFileUrl())
                .fileName(savedAttachment.getFileName())
                .fileType(savedAttachment.getFileType())
                .fileSize(savedAttachment.getFileSize())
                .isImage(savedAttachment.getIsImage())
                .thumbnailUrl(savedAttachment.getThumbnailUrl())
                .build();

        // Add attachment to message response
        if (messageResponse.getAttachments() == null) {
            messageResponse.setAttachments(new ArrayList<>());
        }
        messageResponse.getAttachments().add(attachmentResponse);

        // Notify room about the new attachment
        messagingTemplate.convertAndSend(
                "/topic/room/" + message.getRoom().getId() + "/attachment",
                messageResponse
        );

        return messageResponse;
    }
}