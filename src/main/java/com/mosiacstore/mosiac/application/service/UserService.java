package com.mosiacstore.mosiac.application.service;

import com.mosiacstore.mosiac.application.dto.UserDto;
import com.mosiacstore.mosiac.application.dto.request.UserCreateRequest;
import com.mosiacstore.mosiac.application.dto.request.UserProfileRequest;
import com.mosiacstore.mosiac.application.dto.request.UserUpdateRequest;
import com.mosiacstore.mosiac.application.dto.response.PageResponse;
import com.mosiacstore.mosiac.domain.user.UserStatus;

import java.util.UUID;

public interface UserService {
    // User profile operations
    UserDto getUserProfile(UUID userId);
    UserDto updateUserProfile(UUID userId, UserProfileRequest request);

    // Admin operations
    PageResponse<UserDto> getAllUsers(int page, int size, String sort);
    UserDto getUserById(UUID id);
    UserDto createUser(UserCreateRequest request);
    UserDto updateUser(UUID id, UserUpdateRequest request);
    void deleteUser(UUID id);
    UserDto changeUserStatus(UUID id, UserStatus status);
}