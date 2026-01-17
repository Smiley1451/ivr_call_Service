// ============================================
// FILE: src/main/java/com/labourconnect/service/AudioService.java
// ============================================
package com.labourconnect.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for managing audio file URLs
 */
@Service
@Slf4j
public class AudioService {

    @Value("${twilio.webhook.base.url}")
    private String baseUrl;

    /**
     * Gets audio URL for a specific message key and language
     *
     * @param key Message key (e.g., "welcome", "purpose_selection")
     * @param language Language code (en, hi, kn)
     * @return Full URL to audio file
     */
    public String getAudioUrl(String key, String language) {
        // Construct URL: https://your-ngrok-url.ngrok.io/audio/en/welcome.mp3
        String audioUrl = String.format("%s/Audio/%s/%s.mp3", baseUrl, language, key);
        log.debug("Audio URL: {}", audioUrl);
        return audioUrl;
    }

    /**
     * Check if audio file exists for given key and language
     */
    public boolean audioExists(String key, String language) {
        // For simplicity, assume all audio files exist
        // You could add actual file checking if needed
        return true;
    }
}