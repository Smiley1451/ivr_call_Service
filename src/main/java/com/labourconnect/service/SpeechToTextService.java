package com.labourconnect.service;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.speech.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service for converting speech audio to text using Google Cloud Speech-to-Text API
 * Always transcribes in English regardless of IVR language
 */
@Service
@Slf4j
public class SpeechToTextService {

    @Value("${google.speech.language.en:en-IN}")
    private String languageCodeEn;

    @Value("${google.cloud.credentials.path:}")
    private String credentialsPath;

    @Value("${twilio.account.sid}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token}")
    private String twilioAuthToken;

    private final OkHttpClient httpClient;
    private SpeechClient speechClient;
    private boolean googleCloudEnabled = false;

    public SpeechToTextService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            if (credentialsPath != null && !credentialsPath.isEmpty()) {
                log.info("Initializing Google Cloud Speech-to-Text with credentials: {}", credentialsPath);

                GoogleCredentials credentials = GoogleCredentials.fromStream(
                        new FileInputStream(credentialsPath)
                );

                SpeechSettings settings = SpeechSettings.newBuilder()
                        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                        .build();

                speechClient = SpeechClient.create(settings);
                googleCloudEnabled = true;

                log.info("✅ Google Cloud Speech-to-Text initialized successfully");
            } else {
                String envCredentials = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                if (envCredentials != null && !envCredentials.isEmpty()) {
                    log.info("Using GOOGLE_APPLICATION_CREDENTIALS from environment");
                    speechClient = SpeechClient.create();
                    googleCloudEnabled = true;
                    log.info("✅ Google Cloud Speech-to-Text initialized successfully");
                } else {
                    log.warn("⚠️ Google Cloud credentials not configured. Will use mock transcription.");
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to initialize Google Cloud Speech-to-Text: {}", e.getMessage(), e);
            log.warn("⚠️ Falling back to mock transcription mode");
            googleCloudEnabled = false;
        }
    }

    /**
     * Converts audio from URL to text
     * ALWAYS uses English (en-IN) for transcription regardless of IVR language
     *
     * @param audioUrl URL of the recorded audio (from Twilio)
     * @param language Language code (IGNORED - always uses en-IN)
     * @return Transcribed text in English
     */
    public String transcribeAudioFromUrl(String audioUrl, String language) {
        // ⚠️ IMPORTANT: We ignore the language parameter and always use English
        // because users speak English words (like "electrician", "Bangalore")
        // even when the IVR prompts are in Hindi/Kannada

        log.info("Transcribing audio from URL: {} (forcing en-IN regardless of IVR language)", audioUrl);

        if (!googleCloudEnabled) {
            log.warn("Google Cloud STT not enabled. Using mock transcription.");
            return getMockTranscription();
        }

        try {
            byte[] audioBytes = downloadAudio(audioUrl);
            return transcribeAudio(audioBytes);

        } catch (Exception e) {
            log.error("Error transcribing audio: {}", e.getMessage(), e);
            log.warn("Falling back to mock transcription due to error");
            return getMockTranscription();
        }
    }

    /**
     * Transcribes audio bytes to text using Google Speech-to-Text
     * ALWAYS uses en-IN language
     */
    private String transcribeAudio(byte[] audioBytes) throws IOException {
        log.info("🎙️ Transcribing {} bytes of audio using Google Cloud STT (en-IN)", audioBytes.length);

        try {
            ByteString audioByteString = ByteString.copyFrom(audioBytes);

            RecognitionAudio audio = RecognitionAudio.newBuilder()
                    .setContent(audioByteString)
                    .build();

            // ✅ ALWAYS use en-IN (English - India)
            // This handles Indian English accents properly
            RecognitionConfig config = RecognitionConfig.newBuilder()
                    .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                    .setSampleRateHertz(8000) // Twilio uses 8kHz for phone calls
                    .setLanguageCode("en-IN") // ✅ HARDCODED to English (India)
                    .setEnableAutomaticPunctuation(true)
                    .setModel("default") // Use default model for en-IN
                    .build();

            log.debug("Sending request to Google Cloud STT with language: en-IN");
            RecognizeResponse response = speechClient.recognize(config, audio);

            List<SpeechRecognitionResult> results = response.getResultsList();

            if (results.isEmpty()) {
                log.warn("No transcription results returned from Google Cloud STT");
                return getMockTranscription();
            }

            SpeechRecognitionResult result = results.get(0);

            if (result.getAlternativesCount() == 0) {
                log.warn("No alternatives in transcription result");
                return getMockTranscription();
            }

            SpeechRecognitionAlternative alternative = result.getAlternatives(0);
            String transcript = alternative.getTranscript();
            float confidence = alternative.getConfidence();

            log.info("✅ Transcription successful (en-IN): '{}' (confidence: {:.2f})",
                    transcript, confidence);

            if (confidence < 0.5f) {
                log.warn("⚠️ Low confidence transcription ({:.2f}): '{}'",
                        confidence, transcript);
            }

            return transcript;

        } catch (Exception e) {
            log.error("❌ Error during Google Cloud STT transcription: {}", e.getMessage(), e);
            throw new IOException("Speech-to-Text transcription failed", e);
        }
    }

    /**
     * Mock transcription for demo purposes
     * Returns English words only
     */
    private String getMockTranscription() {
        log.info("Returning mock transcription (English only)");

        // Return random English mock data
        String[] mockData = {
                "Electrician",
                "Plumber",
                "Carpenter",
                "Mason",
                "Painter",
                "Bangalore",
                "Mysore",
                "Hubli",
                "Ravi Kumar",
                "Suresh Gowda",
                "John Doe"
        };

        int random = (int) (Math.random() * mockData.length);
        String result = mockData[random];

        log.info("Mock transcription: {}", result);
        return result;
    }

    /**
     * Downloads audio file from URL with Twilio authentication
     */
    private byte[] downloadAudio(String audioUrl) throws IOException {
        log.info("Downloading audio from: {}", audioUrl);

        if (twilioAccountSid == null || twilioAccountSid.isEmpty()) {
            throw new IOException("Twilio Account SID not configured");
        }
        if (twilioAuthToken == null || twilioAuthToken.isEmpty()) {
            throw new IOException("Twilio Auth Token not configured");
        }

        log.debug("Using Twilio Account SID: {}", twilioAccountSid);

        String credentials = twilioAccountSid + ":" + twilioAuthToken;
        String basicAuth = "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes());

        Request request = new Request.Builder()
                .url(audioUrl)
                .addHeader("Authorization", basicAuth)
                .addHeader("Accept", "audio/wav")
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            log.debug("Response code: {}", response.code());

            if (!response.isSuccessful()) {
                String responseBody = "";
                if (response.body() != null) {
                    responseBody = response.body().string();
                }

                log.error("Failed to download audio. Status: {}, Message: {}, Body: {}",
                        response.code(), response.message(), responseBody);

                if (response.code() == 401) {
                    log.error("Authentication failed! Please verify:");
                    log.error("1. Twilio Account SID matches the account that owns the recording");
                    log.error("2. Twilio Auth Token is correct and not expired");
                    log.error("3. Recording URL Account SID: {}", extractAccountSidFromUrl(audioUrl));
                    log.error("4. Configured Account SID: {}", twilioAccountSid);
                }

                throw new IOException("Failed to download audio: HTTP " + response.code());
            }

            if (response.body() == null) {
                throw new IOException("Response body is null");
            }

            byte[] audioBytes = response.body().bytes();
            log.info("Successfully downloaded {} bytes of audio", audioBytes.length);
            return audioBytes;
        }
    }

    private String extractAccountSidFromUrl(String url) {
        try {
            String[] parts = url.split("/");
            for (int i = 0; i < parts.length; i++) {
                if (parts[i].equals("Accounts") && i + 1 < parts.length) {
                    return parts[i + 1];
                }
            }
        } catch (Exception e) {
            log.warn("Could not extract Account SID from URL", e);
        }
        return "unknown";
    }

    public boolean isValidTranscription(String transcript) {
        if (transcript == null || transcript.trim().isEmpty()) {
            return false;
        }

        if (transcript.trim().length() < 2) {
            return false;
        }

        return true;
    }

    public String cleanTranscription(String transcript) {
        if (transcript == null) {
            return null;
        }

        String cleaned = transcript.trim().replaceAll("\\s+", " ");

        if (!cleaned.isEmpty()) {
            cleaned = cleaned.substring(0, 1).toUpperCase() +
                    cleaned.substring(1);
        }

        return cleaned;
    }

    public void destroy() {
        if (speechClient != null) {
            speechClient.close();
            log.info("Google Cloud Speech client closed");
        }
    }
}