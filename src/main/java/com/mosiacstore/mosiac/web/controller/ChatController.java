package com.mosiacstore.mosiac.web.controller;

import com.mosiacstore.mosiac.application.dto.chat.*;
import com.mosiacstore.mosiac.application.dto.response.ApiResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.service.ChatMessageService;
import com.mosiacstore.mosiac.application.service.ChatRoomService;
import com.mosiacstore.mosiac.domain.chat.MessagePriority;
import com.mosiacstore.mosiac.infrastructure.security.CustomUserDetail;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Chat API")
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final SimpMessagingTemplate messagingTemplate;

    // Chat Room Endpoints

    @Operation(
            summary = "Get user's chat rooms",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatRoomResponse>> getUserChatRooms(
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(chatRoomService.getUserChatRooms(currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Get chat room by ID",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/rooms/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomResponse> getChatRoomById(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(chatRoomService.getRoomById(roomId, currentUser.getUser().getId()));
    }

    @Operation(
            summary = "Create a new chat room",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomResponse> createChatRoom(
            @Valid @RequestBody ChatRoomRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return new ResponseEntity<>(
                chatRoomService.createRoom(request, currentUser.getUser().getId()),
                HttpStatus.CREATED
        );
    }

    @Operation(
            summary = "Get or create private chat room between two users",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/rooms/private/{otherUserId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomResponse> getOrCreatePrivateRoom(
            @PathVariable UUID otherUserId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                chatRoomService.getOrCreatePrivateRoom(currentUser.getUser().getId(), otherUserId)
        );
    }

    @Operation(
            summary = "Delete chat room",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/rooms/{roomId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> deleteChatRoom(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        chatRoomService.deleteRoom(roomId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Chat room deleted successfully"));
    }

    // Chat Message Endpoints

    @Operation(
            summary = "Get messages in a chat room",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/rooms/{roomId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageResponse>> getRoomMessages(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                chatMessageService.getRoomMessages(roomId, currentUser.getUser().getId())
        );
    }

    @Operation(
            summary = "Get paginated messages in a chat room",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/rooms/{roomId}/messages/paged")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PageResponse<ChatMessageResponse>> getPaginatedMessages(
            @PathVariable UUID roomId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                chatMessageService.getPaginatedMessages(roomId, page, size)
        );
    }

    @Operation(
            summary = "Get messages before a timestamp",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/rooms/{roomId}/messages/before")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesBefore(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                chatMessageService.getMessagesBefore(roomId, timestamp, limit)
        );
    }

    @Operation(
            summary = "Get messages after a timestamp",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/rooms/{roomId}/messages/after")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageResponse>> getMessagesAfter(
            @PathVariable UUID roomId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime timestamp,
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                chatMessageService.getMessagesAfter(roomId, timestamp, limit)
        );
    }

    @Operation(
            summary = "Send a message",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageResponse> sendMessage(
            @Valid @RequestBody ChatMessageRequest request,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return new ResponseEntity<>(
                chatMessageService.sendMessage(request, currentUser.getUser().getId()),
                HttpStatus.CREATED
        );
    }

    @Operation(
            summary = "Send a message with attachment",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PostMapping(value = "/messages/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageResponse> sendMessageWithAttachment(
            @RequestParam UUID roomId,
            @RequestParam String content,
            @RequestParam(required = false) MessagePriority priority,
            @RequestParam(required = false) Boolean requiresAttention,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetail currentUser) {

        ChatMessageRequest request = ChatMessageRequest.builder()
                .roomId(roomId)
                .content(content)
                .priority(priority)
                .requiresAttention(requiresAttention)
                .build();

        return new ResponseEntity<>(
                chatMessageService.sendMessageWithAttachment(request, file, currentUser.getUser().getId()),
                HttpStatus.CREATED
        );
    }

    @Operation(
            summary = "Mark message as read",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/messages/{messageId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> markMessageAsRead(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        chatMessageService.markAsRead(messageId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Message marked as read"));
    }

    @Operation(
            summary = "Mark all messages in a room as read",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/rooms/{roomId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> markAllMessagesAsRead(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        chatMessageService.markAllAsReadInRoom(roomId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "All messages marked as read"));
    }

    @Operation(
            summary = "Delete a message",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @DeleteMapping("/messages/{messageId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse> deleteMessage(
            @PathVariable UUID messageId,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        chatMessageService.deleteMessage(messageId, currentUser.getUser().getId());
        return ResponseEntity.ok(new ApiResponse(true, "Message deleted successfully"));
    }

    // Staff/Admin only endpoints

    @Operation(
            summary = "Get all support chat rooms",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin/support-rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PageResponse<ChatRoomResponse>> getSupportRooms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort) {
        return ResponseEntity.ok(chatRoomService.getSupportRooms(page, size, sort));
    }

    @Operation(
            summary = "Get messages requiring attention",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin/messages/requiring-attention")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PageResponse<ChatMessageResponse>> getMessagesRequiringAttention(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(chatMessageService.getMessagesRequiringAttention(page, size));
    }

    @Operation(
            summary = "Get messages by priority",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin/messages/priority/{priority}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<PageResponse<ChatMessageResponse>> getMessagesByPriority(
            @PathVariable MessagePriority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(chatMessageService.getMessagesByPriority(priority, page, size));
    }

    @Operation(
            summary = "Count pending customer messages",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @GetMapping("/admin/messages/pending/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Long> countPendingCustomerMessages() {
        return ResponseEntity.ok(chatMessageService.countPendingCustomerMessages());
    }

    @Operation(
            summary = "Update message priority",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/messages/{messageId}/priority")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ChatMessageResponse> updateMessagePriority(
            @PathVariable UUID messageId,
            @RequestParam MessagePriority priority,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                chatMessageService.updateMessagePriority(messageId, priority, currentUser.getUser().getId())
        );
    }

    @Operation(
            summary = "Flag message for attention",
            security = @SecurityRequirement(name = "Bearer Authentication")
    )
    @PutMapping("/admin/messages/{messageId}/attention")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ChatMessageResponse> flagMessageForAttention(
            @PathVariable UUID messageId,
            @RequestParam Boolean requiresAttention,
            @AuthenticationPrincipal CustomUserDetail currentUser) {
        return ResponseEntity.ok(
                chatMessageService.flagForAttention(messageId, requiresAttention, currentUser.getUser().getId())
        );
    }

    // WebSocket handlers

    @MessageMapping("/chat.sendMessage")
    public void handleSendMessage(@Payload ChatMessageRequest request,
                                  @AuthenticationPrincipal CustomUserDetail currentUser) {
        ChatMessageResponse response = chatMessageService.sendMessage(request, currentUser.getUser().getId());
        messagingTemplate.convertAndSend("/topic/room/" + request.getRoomId(), response);
    }

    @MessageMapping("/chat.markAsRead")
    public void handleMarkAsRead(@Payload ChatMessageRequest request,
                                 @AuthenticationPrincipal CustomUserDetail currentUser) {
        chatMessageService.markAsRead(request.getRoomId(), currentUser.getUser().getId());
    }
}