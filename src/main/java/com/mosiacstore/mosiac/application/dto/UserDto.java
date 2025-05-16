package com.mosiacstore.mosiac.application.dto;

import com.mosiacstore.mosiac.domain.user.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private UUID id;
    private String email;
    private String phoneNumber;
    private String fullName;
    private UserRole role;
}
