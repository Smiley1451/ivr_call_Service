package com.labourconnect.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.SpeechSettings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * Google Cloud Speech-to-Text configuration
 */
@Configuration
@Slf4j
public class GoogleCloudConfig {

    @Value("${google.cloud.credentials.path}")
    private String credentialsPath;

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        log.info("Loading Google Cloud credentials from: {}", credentialsPath);

        try (FileInputStream serviceAccountStream = new FileInputStream(credentialsPath)) {
            return GoogleCredentials.fromStream(serviceAccountStream);
        }
    }

    @Bean
    public SpeechSettings speechSettings(GoogleCredentials credentials) throws IOException {
        log.info("Configuring Google Speech-to-Text settings");

        return SpeechSettings.newBuilder()
                .setCredentialsProvider(() -> credentials)
                .build();
    }
}
