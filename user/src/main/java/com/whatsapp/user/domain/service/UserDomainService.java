package com.whatsapp.user.domain.service;

import com.whatsapp.user.domain.model.vo.Email;
import com.whatsapp.user.domain.model.vo.PhoneNumber;
import com.whatsapp.user.domain.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * User Domain Service
 *
 * Contains domain logic that doesn't naturally fit within a single entity.
 * Coordinates between multiple entities or enforces cross-entity business rules.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDomainService {

    private final UserRepository userRepository;

    /**
     * Validate user uniqueness constraints
     *
     * Business Rule: Username, email, and phone number must be unique across all users
     */
    public void validateUserUniqueness(String username, Email email, PhoneNumber phoneNumber) {
        log.debug("Validating uniqueness for username: {}, email: {}, phone: {}",
                username, email.getValue(), phoneNumber.getValue());

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email.getValue());
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new IllegalArgumentException("Phone number already exists: " + phoneNumber.getValue());
        }

        log.debug("User uniqueness validation passed");
    }

    /**
     * Check if user can be deleted
     *
     * Business Rule: User can only be deleted if they have no active conversations
     * (This would check with Chat Service in a real implementation)
     */
    public boolean canDeleteUser(String userId) {
        // TODO: Check with Chat Service if user has active conversations
        // For now, always return true
        return true;
    }

    /**
     * Validate password strength
     *
     * Business Rule: Password must meet security requirements
     */
    public void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLowercase = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(ch) >= 0);

        if (!hasUppercase) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!hasLowercase) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!hasDigit) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
        if (!hasSpecial) {
            throw new IllegalArgumentException("Password must contain at least one special character");
        }
    }
}