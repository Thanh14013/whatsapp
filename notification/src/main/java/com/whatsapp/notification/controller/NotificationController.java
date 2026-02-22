package com.whatsapp.notification.controller;

import com.whatsapp.common.dto.BaseResponse;
import com.whatsapp.notification.domain.model.DeviceToken;
import com.whatsapp.notification.dto.RegisterTokenRequest;
import com.whatsapp.notification.dto.SendNotificationRequest;
import com.whatsapp.notification.service.DeviceTokenService;
import com.whatsapp.notification.service.NotificationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Notification REST Controller
 *
 * Handles HTTP requests for notification operations.
 *
 * Endpoints:
 * - POST /notifications/register - Register device token
 * - DELETE /notifications/token/{token} - Remove device token
 * - POST /notifications/send - Send notification (manual trigger)
 * - GET /notifications/tokens/{userId} - Get user tokens
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final DeviceTokenService deviceTokenService;

    /**
     * Register device token
     *
     * POST /notifications/register
     */
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> registerToken(
            @Valid @RequestBody RegisterTokenRequest request) {

        log.info("Registering device token for user: {}", request.getUserId());

        deviceTokenService.registerToken(
                request.getUserId(),
                request.getToken(),
                request.getPlatform()
        );

        return ResponseEntity.ok(BaseResponse.success("Device token registered successfully"));
    }

    /**
     * Remove device token
     *
     * DELETE /notifications/token/{token}
     */
    @DeleteMapping("/token/{token}")
    public ResponseEntity<BaseResponse<Void>> removeToken(@PathVariable String token) {
        log.info("Removing device token: {}", token);

        deviceTokenService.removeToken(token);

        return ResponseEntity.ok(BaseResponse.success("Device token removed successfully"));
    }

    /**
     * Remove all tokens for user
     *
     * DELETE /notifications/user/{userId}/tokens
     */
    @DeleteMapping("/user/{userId}/tokens")
    public ResponseEntity<BaseResponse<Void>> removeAllTokens(@PathVariable String userId) {
        log.info("Removing all tokens for user: {}", userId);

        deviceTokenService.removeAllTokensForUser(userId);

        return ResponseEntity.ok(BaseResponse.success("All device tokens removed successfully"));
    }

    /**
     * Get all tokens for user
     *
     * GET /notifications/tokens/{userId}
     */
    @GetMapping("/tokens/{userId}")
    public ResponseEntity<BaseResponse<Set<String>>> getUserTokens(@PathVariable String userId) {
        log.debug("Getting tokens for user: {}", userId);

        Set<String> tokens = deviceTokenService.getTokensForUser(userId);

        return ResponseEntity.ok(BaseResponse.success(tokens));
    }

    /**
     * Send notification manually (for testing)
     *
     * POST /notifications/send
     */
    @PostMapping("/send")
    public ResponseEntity<BaseResponse<Void>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {

        log.info("Sending notification to user: {}", request.getUserId());

        notificationService.sendMessageNotification(
                request.getUserId(),
                request.getSenderName(),
                request.getMessage()
        );

        return ResponseEntity.ok(BaseResponse.success("Notification sent successfully"));
    }
}