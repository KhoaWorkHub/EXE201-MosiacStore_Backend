package com.mosiacstore.mosiac.application.dto.request;

import com.mosiacstore.mosiac.domain.user.UserRole;
import com.mosiacstore.mosiac.domain.user.UserStatus;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserUpdateRequest {
    private String fullName;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "Phone number must be 10-11 digits")
    private String phoneNumber;

    private UserRole role;

    private UserStatus status;
}