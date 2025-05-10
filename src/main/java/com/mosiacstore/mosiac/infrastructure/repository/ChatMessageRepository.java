package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.chat.ChatMessage;
import com.mosiacstore.mosiac.domain.chat.MessagePriority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(UUID roomId);

    Page<ChatMessage> findByRoomId(UUID roomId, Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.room.id = :roomId AND m.createdAt < :timestamp")
    List<ChatMessage> findMessagesBefore(@Param("roomId") UUID roomId,
                                         @Param("timestamp") LocalDateTime timestamp,
                                         Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.room.id = :roomId AND m.createdAt > :timestamp")
    List<ChatMessage> findMessagesAfter(@Param("roomId") UUID roomId,
                                        @Param("timestamp") LocalDateTime timestamp,
                                        Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.requiresAttention = true")
    Page<ChatMessage> findMessagesRequiringAttention(Pageable pageable);

    @Query("SELECT m FROM ChatMessage m WHERE m.priority = :priority")
    Page<ChatMessage> findByPriority(@Param("priority") MessagePriority priority, Pageable pageable);

    @Query("SELECT COUNT(m) FROM ChatMessage m JOIN m.room r WHERE r.type = 'SUPPORT' AND " +
            "NOT EXISTS (SELECT rs FROM MessageReadStatus rs WHERE rs.message = m AND rs.user.role = 'STAFF')")
    long countPendingCustomerMessages();
}