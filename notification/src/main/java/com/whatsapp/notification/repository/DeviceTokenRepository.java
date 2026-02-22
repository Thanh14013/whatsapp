package com.whatsapp.notification.repository;

import com.whatsapp.notification.domain.model.DeviceToken;

import java.util.Optional;
import java.util.Set;

/**
 * Device Token Repository Interface
 *
 * Repository interface for device token operations.
 * Implementation uses Redis for storage.
 *
 * @author WhatsApp Clone Team
 */
public interface DeviceTokenRepository {

    /**
     * Save device token
     */
    void save(DeviceToken deviceToken);

    /**
     * Find token by value
     */
    Optional<DeviceToken> findByToken(String token);

    /**
     * Find all tokens for user
     */
    Set<String> findByUserId(String userId);

    /**
     * Delete token
     */
    void deleteByToken(String token);

    /**
     * Delete all tokens for user
     */
    void deleteByUserId(String userId);

    /**
     * Check if token exists
     */
    boolean existsByToken(String token);

    /**
     * Count tokens for user
     */
    long countByUserId(String userId);
}