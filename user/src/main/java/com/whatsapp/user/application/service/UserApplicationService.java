package com.whatsapp.user.application.service;

import com.whatsapp.user.application.dto.CreateUserRequest;
import com.whatsapp.user.application.dto.UpdateUserRequest;
import com.whatsapp.user.application.dto.UserDto;
import com.whatsapp.user.application.mapper.UserMapper;
import com.whatsapp.user.domain.model.User;
import com.whatsapp.user.domain.model.UserProfile;
import com.whatsapp.user.domain.model.UserStatus;
import com.whatsapp.user.domain.model.vo.Email;
import com.whatsapp.user.domain.model.vo.PhoneNumber;
import com.whatsapp.user.domain.model.vo.UserId;
import com.whatsapp.user.domain.repository.UserRepository;
import com.whatsapp.user.domain.service.UserDomainService;
import com.whatsapp.user.infrastructure.cache.UserCacheService;
import com.whatsapp.user.infrastructure.messaging.UserEventPublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * User Application Service
 *
 * Orchestrates user-related use cases.
 * Coordinates between domain services, repositories, and infrastructure.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserApplicationService {

    private final UserRepository userRepository;
    private final UserDomainService userDomainService;
    private final UserCacheService cacheService;
    private final UserEventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    /**
     * Create new user
     */
    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        log.info("Creating new user with username: {}", request.getUsername());

        // Validate uniqueness
        userDomainService.validateUserUniqueness(
                request.getUsername(),
                Email.of(request.getEmail()),
                PhoneNumber.of(request.getPhoneNumber())
        );

        // Encode password
        String passwordHash = passwordEncoder.encode(request.getPassword());

        // Create user profile
        UserProfile profile = UserProfile.create(
                request.getDisplayName(),
                request.getBio(),
                null,
                request.getPhoneNumber()
        );

        // Create user
        User user = User.create(
                request.getUsername(),
                Email.of(request.getEmail()),
                PhoneNumber.of(request.getPhoneNumber()),
                passwordHash,
                profile
        );

        // Save user
        User savedUser = userRepository.save(user);

        // Publish event
        eventPublisher.publishUserCreated(savedUser);

        log.info("User created successfully with ID: {}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public UserDto getUserById(String userId) {
        log.debug("Getting user by ID: {}", userId);

        // Try cache first
        UserDto cachedUser = cacheService.getUser(userId);
        if (cachedUser != null) {
            log.debug("User found in cache: {}", userId);
            return cachedUser;
        }

        // Load from database
        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        UserDto userDto = userMapper.toDto(user);

        // Update cache
        cacheService.cacheUser(userDto);

        return userDto;
    }

    /**
     * Update user profile
     */
    @Transactional
    public UserDto updateUser(String userId, UpdateUserRequest request) {
        log.info("Updating user: {}", userId);

        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        // Update profile
        UserProfile updatedProfile = user.getProfile()
                .withDisplayName(request.getDisplayName())
                .withBio(request.getBio())
                .withStatusMessage(request.getStatusMessage());

        user.updateProfile(updatedProfile);

        // Save
        User updatedUser = userRepository.save(user);

        // Invalidate cache
        cacheService.evictUser(userId);

        // Publish event
        eventPublisher.publishUserUpdated(updatedUser);

        log.info("User updated successfully: {}", userId);

        return userMapper.toDto(updatedUser);
    }

    /**
     * Change user status
     */
    @Transactional
    public void changeUserStatus(String userId, UserStatus newStatus) {
        log.debug("Changing user status: {} to {}", userId, newStatus);

        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.changeStatus(newStatus);
        userRepository.save(user);

        // Update cache
        cacheService.updateUserStatus(userId, newStatus);

        // Publish event
        eventPublisher.publishUserStatusChanged(user);
    }

    /**
     * Search users by username
     */
    @Transactional(readOnly = true)
    public List<UserDto> searchUsers(String query, int limit) {
        log.debug("Searching users with query: {}, limit: {}", query, limit);

        List<User> users = userRepository.searchByUsername(query, limit);

        return users.stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Deactivate user
     */
    @Transactional
    public void deactivateUser(String userId) {
        log.info("Deactivating user: {}", userId);

        User user = userRepository.findById(UserId.of(userId))
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.deactivate();
        userRepository.save(user);

        // Invalidate cache
        cacheService.evictUser(userId);

        // Publish event
        eventPublisher.publishUserDeactivated(user);

        log.info("User deactivated: {}", userId);
    }
}