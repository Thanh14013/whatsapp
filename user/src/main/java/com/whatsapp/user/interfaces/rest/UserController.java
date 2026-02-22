package com.whatsapp.user.interfaces.rest;

import com.whatsapp.user.application.dto.CreateUserRequest;
import com.whatsapp.user.application.dto.UpdateUserRequest;
import com.whatsapp.user.application.dto.UserDto;
import com.whatsapp.user.application.service.UserApplicationService;
import com.whatsapp.user.domain.model.UserStatus;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * User REST Controller
 *
 * Handles HTTP requests for user operations.
 *
 * Endpoints:
 * - POST /users - Create new user
 * - GET /users/me - Get current user
 * - GET /users/{id} - Get user by ID
 * - PUT /users/{id} - Update user
 * - DELETE /users/{id} - Delete user
 * - PUT /users/{id}/status - Update user status
 * - GET /users/search - Search users
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserApplicationService userService;

    /**
     * Create new user
     *
     * POST /users
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody CreateUserRequest request) {
        log.info("Creating new user: {}", request.getUsername());

        UserDto user = userService.createUser(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    /**
     * Get current authenticated user
     *
     * GET /users/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        String userId = authentication.getName(); // User ID from JWT
        log.debug("Getting current user: {}", userId);

        UserDto user = userService.getUserById(userId);

        return ResponseEntity.ok(user);
    }

    /**
     * Get user by ID
     *
     * GET /users/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable String id) {
        log.debug("Getting user by ID: {}", id);

        UserDto user = userService.getUserById(id);

        return ResponseEntity.ok(user);
    }

    /**
     * Update user
     *
     * PUT /users/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request,
            Authentication authentication) {

        String currentUserId = authentication.getName();

        // Check if user is updating their own profile
        if (!id.equals(currentUserId)) {
            log.warn("User {} attempted to update user {}", currentUserId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Updating user: {}", id);

        UserDto user = userService.updateUser(id, request);

        return ResponseEntity.ok(user);
    }

    /**
     * Delete user (deactivate)
     *
     * DELETE /users/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String id,
            Authentication authentication) {

        String currentUserId = authentication.getName();

        // Check if user is deleting their own account
        if (!id.equals(currentUserId)) {
            log.warn("User {} attempted to delete user {}", currentUserId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.info("Deactivating user: {}", id);

        userService.deactivateUser(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Update user status
     *
     * PUT /users/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable String id,
            @RequestParam UserStatus status,
            Authentication authentication) {

        String currentUserId = authentication.getName();

        // Check if user is updating their own status
        if (!id.equals(currentUserId)) {
            log.warn("User {} attempted to update status of user {}", currentUserId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        log.debug("Updating user status: {} to {}", id, status);

        userService.changeUserStatus(id, status);

        return ResponseEntity.ok().build();
    }

    /**
     * Search users
     *
     * GET /users/search?q=query&limit=10
     */
    @GetMapping("/search")
    public ResponseEntity<List<UserDto>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit) {

        log.debug("Searching users with query: {}, limit: {}", q, limit);

        List<UserDto> users = userService.searchUsers(q, limit);

        return ResponseEntity.ok(users);
    }
}