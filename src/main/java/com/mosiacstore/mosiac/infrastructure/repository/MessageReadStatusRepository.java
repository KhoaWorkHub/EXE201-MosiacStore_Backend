package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.chat.MessageReadStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MessageReadStatusRepository extends JpaRepository<MessageReadStatus, UUID> {

    @Query("SELECT mrs FROM MessageReadStatus mrs WHERE mrs.message.id = :messageId AND mrs.user.id = :userId")
    Optional<MessageReadStatus> findByMessageIdAndUserId(@Param("messageId") UUID messageId,
                                                         @Param("userId") UUID userId);

    @Query("SELECT mrs FROM MessageReadStatus mrs WHERE mrs.message.room.id = :roomId AND mrs.user.id = :userId")
    List<MessageReadStatus> findByRoomIdAndUserId(@Param("roomId") UUID roomId,
                                                  @Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE MessageReadStatus mrs SET mrs.isRead = true WHERE mrs.message.room.id = :roomId AND mrs.user.id = :userId")
    void markAllAsReadInRoom(@Param("roomId") UUID roomId, @Param("userId") UUID userId);
}