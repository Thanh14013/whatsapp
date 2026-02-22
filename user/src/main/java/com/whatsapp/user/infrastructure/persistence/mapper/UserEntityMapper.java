package com.whatsapp.user.infrastructure.persistence.mapper;

import com.whatsapp.user.domain.model.User;
import com.whatsapp.user.domain.model.UserProfile;
import com.whatsapp.user.domain.model.vo.Email;
import com.whatsapp.user.domain.model.vo.PhoneNumber;
import com.whatsapp.user.domain.model.vo.UserId;
import com.whatsapp.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

/**
 * User Entity Mapper
 *
 * Maps between User domain model and UserEntity (JPA entity).
 * Manual mapping since MapStruct cannot handle complex value objects easily.
 *
 * @author WhatsApp Clone Team
 */
@Component
public class UserEntityMapper {

    /**
     * Convert User domain model to UserEntity
     */
    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }

        return UserEntity.builder()
                .id(user.getId().getValue())
                .username(user.getUsername())
                .email(user.getEmail().getValue())
                .phoneNumber(user.getPhoneNumber().getValue())
                .passwordHash(user.getPasswordHash())
                .displayName(user.getProfile().getDisplayName())
                .bio(user.getProfile().getBio())
                .avatarUrl(user.getProfile().getAvatarUrl())
                .statusMessage(user.getProfile().getStatusMessage())
                .status(user.getStatus())
                .active(user.isActive())
                .emailVerified(user.isEmailVerified())
                .phoneVerified(user.isPhoneVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastSeenAt(user.getUpdatedAt())
                .build();
    }

    /**
     * Convert UserEntity to User domain model
     */
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        UserProfile profile = UserProfile.create(
                entity.getDisplayName(),
                entity.getBio(),
                entity.getAvatarUrl(),
                entity.getPhoneNumber()
        );

        if (entity.getStatusMessage() != null) {
            profile = profile.withStatusMessage(entity.getStatusMessage());
        }

        return new User(
                UserId.of(entity.getId()),
                entity.getUsername(),
                Email.of(entity.getEmail()),
                PhoneNumber.of(entity.getPhoneNumber()),
                entity.getPasswordHash(),
                profile,
                entity.getStatus(),
                entity.getActive(),
                entity.getEmailVerified(),
                entity.getPhoneVerified(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}