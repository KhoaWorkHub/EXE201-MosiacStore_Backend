package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.chat.ChatRoom;
import com.mosiacstore.mosiac.domain.chat.ChatRoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("SELECT r FROM ChatRoom r JOIN r.participants p WHERE p.id = :userId")
    List<ChatRoom> findByParticipantId(@Param("userId") UUID userId);

    @Query("SELECT r FROM ChatRoom r JOIN r.participants p1 JOIN r.participants p2 " +
            "WHERE r.type = 'PRIVATE' AND p1.id = :user1Id AND p2.id = :user2Id")
    Optional<ChatRoom> findPrivateRoom(@Param("user1Id") UUID user1Id, @Param("user2Id") UUID user2Id);

    @Query("SELECT r FROM ChatRoom r WHERE r.type = :type")
    Page<ChatRoom> findByType(@Param("type") ChatRoomType type, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.room.id = :roomId AND m.status = 'SENT'")
    long countUnreadMessagesInRoom(@Param("roomId") UUID roomId);
}