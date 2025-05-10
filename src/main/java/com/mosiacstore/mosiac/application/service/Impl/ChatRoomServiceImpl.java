package com.mosiacstore.mosiac.application.service.Impl;

import com.mosiacstore.mosiac.application.dto.chat.*;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.application.exception.EntityNotFoundException;
import com.mosiacstore.mosiac.application.exception.InvalidOperationException;
import com.mosiacstore.mosiac.application.mapper.ChatMapper;
import com.mosiacstore.mosiac.application.service.ChatRoomService;
import com.mosiacstore.mosiac.domain.chat.ChatRoom;
import com.mosiacstore.mosiac.domain.chat.ChatRoomType;
import com.mosiacstore.mosiac.domain.user.User;
import com.mosiacstore.mosiac.infrastructure.config.ChatChannelInterceptor;
import com.mosiacstore.mosiac.infrastructure.repository.ChatMessageRepository;
import com.mosiacstore.mosiac.infrastructure.repository.ChatRoomRepository;
import com.mosiacstore.mosiac.infrastructure.repository.MessageReadStatusRepository;
import com.mosiacstore.mosiac.infrastructure.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageReadStatusRepository readStatusRepository;
    private final ChatMapper chatMapper;
    private final ChatChannelInterceptor chatInterceptor;

    @Override
    public List<ChatRoomResponse> getUserChatRooms(UUID userId) {
        List<ChatRoom> rooms = chatRoomRepository.findByParticipantId(userId);
        return rooms.stream()
                .map(room -> {
                    ChatRoomResponse response = chatMapper.toChatRoomResponse(room);
                    response.setUnreadCount((int) countUnreadMessages(room.getId(), userId));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Override
    public ChatRoomResponse getRoomById(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + roomId));

        // Verify user is a participant
        boolean isParticipant = room.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(userId));

        if (!isParticipant) {
            throw new InvalidOperationException("User is not a participant in this chat room");
        }

        ChatRoomResponse response = chatMapper.toChatRoomResponse(room);
        response.setUnreadCount((int) countUnreadMessages(roomId, userId));
        return response;
    }

    @Override
    @Transactional
    public ChatRoomResponse createRoom(ChatRoomRequest request, UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + creatorId));

        // Create the chat room
        ChatRoom room = new ChatRoom();
        room.setName(request.getName());
        room.setType(request.getType());
        room.setIsActive(true);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());

        // Add participants
        Set<User> participants = new HashSet<>();
        participants.add(creator);

        if (request.getParticipantIds() != null && !request.getParticipantIds().isEmpty()) {
            for (UUID participantId : request.getParticipantIds()) {
                if (!participantId.equals(creatorId)) {
                    User participant = userRepository.findById(participantId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + participantId));
                    participants.add(participant);
                }
            }
        }

        room.setParticipants(participants);
        ChatRoom savedRoom = chatRoomRepository.save(room);

        return chatMapper.toChatRoomResponse(savedRoom);
    }

    @Override
    @Transactional
    public ChatRoomResponse getOrCreatePrivateRoom(UUID user1Id, UUID user2Id) {
        // Check if private room already exists
        Optional<ChatRoom> existingRoom = chatRoomRepository.findPrivateRoom(user1Id, user2Id);

        if (existingRoom.isPresent()) {
            ChatRoomResponse response = chatMapper.toChatRoomResponse(existingRoom.get());
            response.setUnreadCount((int) countUnreadMessages(existingRoom.get().getId(), user1Id));
            return response;
        }

        // Get users
        User user1 = userRepository.findById(user1Id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + user1Id));

        User user2 = userRepository.findById(user2Id)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + user2Id));

        // Create new private room
        ChatRoom newRoom = new ChatRoom();
        newRoom.setName(user1.getFullName() + " & " + user2.getFullName());
        newRoom.setType(ChatRoomType.PRIVATE);
        newRoom.setIsActive(true);
        newRoom.setCreatedAt(LocalDateTime.now());
        newRoom.setUpdatedAt(LocalDateTime.now());

        // Add participants
        Set<User> participants = new HashSet<>();
        participants.add(user1);
        participants.add(user2);
        newRoom.setParticipants(participants);

        ChatRoom savedRoom = chatRoomRepository.save(newRoom);
        return chatMapper.toChatRoomResponse(savedRoom);
    }

    @Override
    public PageResponse<ChatRoomResponse> getSupportRooms(int page, int size, String sort) {
        Sort sorting = Sort.by(Sort.Direction.DESC, "updatedAt");
        if (sort != null && !sort.isEmpty()) {
            String[] sortParams = sort.split(",");
            Sort.Direction direction = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sorting = Sort.by(direction, sortParams[0]);
        }

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<ChatRoom> roomPage = chatRoomRepository.findByType(ChatRoomType.SUPPORT, pageable);

        List<ChatRoomResponse> roomResponses = roomPage.getContent().stream()
                .map(chatMapper::toChatRoomResponse)
                .collect(Collectors.toList());

        return new PageResponse<>(
                roomResponses,
                roomPage.getNumber(),
                roomPage.getSize(),
                roomPage.getTotalElements(),
                roomPage.getTotalPages(),
                roomPage.isFirst(),
                roomPage.isLast()
        );
    }

    @Override
    public long countUnreadMessages(UUID roomId, UUID userId) {
        return chatRoomRepository.countUnreadMessagesInRoom(roomId);
    }

    @Override
    @Transactional
    public void deleteRoom(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + roomId));

        // Verify user is a participant with proper permissions
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        boolean isParticipant = room.getParticipants().stream()
                .anyMatch(participant -> participant.getId().equals(userId));

        if (!isParticipant && !user.getRole().name().equals("ADMIN") && !user.getRole().name().equals("STAFF")) {
            throw new InvalidOperationException("User does not have permission to delete this chat room");
        }

        chatRoomRepository.delete(room);
    }

    @Override
    @Transactional
    public void addParticipant(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + roomId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        room.getParticipants().add(user);
        chatRoomRepository.save(room);
    }

    @Override
    @Transactional
    public void removeParticipant(UUID roomId, UUID userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + roomId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId));

        room.getParticipants().remove(user);
        chatRoomRepository.save(room);
    }

    @Override
    public List<UUID> getActiveRoomParticipants(UUID roomId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new EntityNotFoundException("Chat room not found with ID: " + roomId));

        Map<String, String> activeUsers = chatInterceptor.getActiveUsers();

        return room.getParticipants().stream()
                .filter(participant -> activeUsers.containsValue(participant.getEmail()))
                .map(User::getId)
                .collect(Collectors.toList());
    }
}