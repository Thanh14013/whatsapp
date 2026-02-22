package com.whatsapp.user.infrastructure.persistence.repository;

import com.whatsapp.user.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User JPA Repository
 *
 * Spring Data JPA repository for UserEntity.
 * Provides database access methods.
 *
 * @author WhatsApp Clone Team
 */
@Repository
public interface UserJpaRepository extends JpaRepository<UserEntity, String> {

    /**
     * Find user by username
     */
    Optional<UserEntity> findByUsername(String username);

    /**
     * Find user by email
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * Find user by phone number
     */
    Optional<UserEntity> findByPhoneNumber(String phoneNumber);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Check if phone number exists
     */
    boolean existsByPhoneNumber(String phoneNumber);

    /**
     * Find all active users
     */
    List<UserEntity> findByActiveTrue();

    /**
     * Search users by username pattern (case-insensitive)
     */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :pattern, '%')) AND u.active = true")
    List<UserEntity> searchByUsername(@Param("pattern") String pattern);

    /**
     * Find users by username pattern with limit
     */
    @Query("SELECT u FROM UserEntity u WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :pattern, '%')) AND u.active = true ORDER BY u.username")
    List<UserEntity> findTopByUsernameContainingIgnoreCase(@Param("pattern") String pattern);
}