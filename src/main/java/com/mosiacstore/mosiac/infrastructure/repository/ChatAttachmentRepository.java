package com.mosiacstore.mosiac.infrastructure.repository;

import com.mosiacstore.mosiac.domain.chat.ChatAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatAttachmentRepository extends JpaRepository<ChatAttachment, UUID> {

    List<ChatAttachment> findByMessageId(UUID messageId);
}