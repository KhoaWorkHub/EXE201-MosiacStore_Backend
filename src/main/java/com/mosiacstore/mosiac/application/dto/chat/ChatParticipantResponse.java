package com.mosiacstore.mosiac.application.dto.chat;

import com.mosiacstore.mosiac.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantResponse {
    private UUID id;
    private String email;
    private String fullName;
    private UserRole role;
    private Boolean isOnline;
}