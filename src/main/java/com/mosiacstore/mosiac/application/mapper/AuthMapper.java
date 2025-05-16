package com.mosiacstore.mosiac.application.mapper;

import com.mosiacstore.mosiac.application.dto.UserDto;
import com.mosiacstore.mosiac.domain.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthMapper {
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    UserDto toUserDto(User user);
}