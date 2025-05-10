package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.chat.ChatRoomRequest;
import com.mosiacstore.mosiac.application.dto.chat.ChatRoomResponse;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;

import java.util.List;
import java.util.UUID;

public interface ChatRoomService {

    List<ChatRoomResponse> getUserChatRooms(UUID userId);

    ChatRoomResponse getRoomById(UUID roomId, UUID userId);

    ChatRoomResponse createRoom(ChatRoomRequest request, UUID creatorId);

    ChatRoomResponse getOrCreatePrivateRoom(UUID user1Id, UUID user2Id);

    PageResponse<ChatRoomResponse> getSupportRooms(int page, int size, String sort);

    long countUnreadMessages(UUID roomId, UUID userId);

    void deleteRoom(UUID roomId, UUID userId);

    void addParticipant(UUID roomId, UUID userId);

    void removeParticipant(UUID roomId, UUID userId);

    List<UUID> getActiveRoomParticipants(UUID roomId);
}