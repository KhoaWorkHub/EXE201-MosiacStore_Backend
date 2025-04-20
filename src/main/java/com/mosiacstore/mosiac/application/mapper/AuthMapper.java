package com.mosiacstore.mosiac.application.mapper;

import com.mosiacstore.mosiac.application.dto.UserDto;
import com.mosiacstore.mosiac.domain.user.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    UserDto toUserDto(User user);
}
