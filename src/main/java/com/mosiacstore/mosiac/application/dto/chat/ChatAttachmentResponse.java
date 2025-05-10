package com.mosiacstore.mosiac.application.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachmentResponse {
    private UUID id;
    private UUID messageId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Boolean isImage;
    private String thumbnailUrl;
}