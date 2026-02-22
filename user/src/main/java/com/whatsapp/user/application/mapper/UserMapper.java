package com.whatsapp.user.application.mapper;

import com.whatsapp.user.application.dto.UserDto;
import com.whatsapp.user.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * User Mapper
 *
 * MapStruct mapper to convert between domain models and DTOs.
 *
 * @author WhatsApp Clone Team
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * Convert User domain model to UserDto
     */
    @Mapping(source = "id.value", target = "id")
    @Mapping(source = "email.value", target = "email")
    @Mapping(source = "phoneNumber.value", target = "phoneNumber")
    @Mapping(source = "profile.displayName", target = "displayName")
    @Mapping(source = "profile.bio", target = "bio")
    @Mapping(source = "profile.avatarUrl", target = "avatarUrl")
    @Mapping(source = "profile.statusMessage", target = "statusMessage")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "active", target = "active")
    @Mapping(source = "emailVerified", target = "emailVerified")
    @Mapping(source = "phoneVerified", target = "phoneVerified")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "updatedAt", target = "updatedAt")
    @Mapping(target = "lastSeenAt", source = "updatedAt")
    UserDto toDto(User user);
}