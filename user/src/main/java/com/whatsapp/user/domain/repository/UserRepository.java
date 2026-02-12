package com.whatsapp.user.domain.repository;

import com.whatsapp.user.domain.model.User;
import com.whatsapp.user.domain.model.vo.Email;
import com.whatsapp.user.domain.model.vo.PhoneNumber;
import com.whatsapp.user.domain.model.vo.UserId;

import java.util.List;
import java.util.Optional;

/**
 * User Repository Interface (Domain Layer)
 *
 * Defines repository operations for User aggregate.
 * Implementation is in infrastructure layer.
 *
 * @author WhatsApp Clone Team
 */
public interface UserRepository {

    /**
     * Save or update user
     */
    User save(User user);

    /**
     * Find user by ID
     */
    Optional<User> findById(UserId userId);

    /**
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<User> findByEmail(Email email);

    /**
     * Find user by phone number
     */
    Optional<User> findByPhoneNumber(PhoneNumber phoneNumber);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(Email email);

    /**
     * Check if phone number exists
     */
    boolean existsByPhoneNumber(PhoneNumber phoneNumber);

    /**
     * Search users by username pattern
     */
    List<User> searchByUsername(String usernamePattern, int limit);

    /**
     * Find all active users
     */
    List<User> findAllActive();

    /**
     * Delete user (soft delete)
     */
    void delete(UserId userId);
}