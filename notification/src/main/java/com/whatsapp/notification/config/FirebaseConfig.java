package com.whatsapp.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Firebase Configuration
 *
 * Configures Firebase Admin SDK for FCM.
 *
 * @author WhatsApp Clone Team
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.database.url:}")
    private String databaseUrl;

    /**
     * Initialize Firebase App
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        log.info("Initializing Firebase App...");

        // Check if already initialized
        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("Firebase App already initialized");
            return FirebaseApp.getInstance();
        }

        FirebaseOptions options;

        if (credentialsPath != null && !credentialsPath.isEmpty()) {
            // Initialize with service account file
            log.info("Initializing Firebase with credentials file: {}", credentialsPath);

            FileInputStream serviceAccount = new FileInputStream(credentialsPath);

            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl(databaseUrl)
                    .build();
        } else {
            // Initialize with default credentials (for local development)
            log.warn("No Firebase credentials path configured. Using default credentials.");
            log.warn("For production, please configure firebase.credentials.path");

            // Use mock credentials for local development
            options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(
                            new ByteArrayInputStream("{}".getBytes())))
                    .build();
        }

        FirebaseApp app = FirebaseApp.initializeApp(options);

        log.info("Firebase App initialized successfully");

        return app;
    }

    /**
     * Create FirebaseMessaging bean
     */
    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        log.info("Creating FirebaseMessaging bean");
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}