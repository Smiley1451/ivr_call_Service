// ============================================
// FILE: src/main/java/com/labourconnect/service/WebSocketLogService.java
// ============================================
package com.labourconnect.service;

import com.labourconnect.dto.CallLogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Service to broadcast call events to WebSocket dashboard
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WebSocketLogService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast a call event to all connected dashboards
     */
    public void broadcastEvent(CallLogEvent event) {
        try {
            event.setTimestamp(LocalDateTime.now());
            messagingTemplate.convertAndSend("/topic/call-logs", event);
            log.debug("Broadcasted event: {}", event.getEventType());
        } catch (Exception e) {
            log.error("Failed to broadcast event: {}", e.getMessage());
        }
    }

    // Convenience methods for common events

    public void logCallStart(String callSid, String phoneNo) {
        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .phoneNo(phoneNo)
                .eventType("CALL_START")
                .message("📞 New call received from " + phoneNo)
                .status("INFO")
                .build());
    }

    public void logLanguageSelected(String callSid, String language) {
        String langName = switch (language) {
            case "hi" -> "Hindi";
            case "kn" -> "Kannada";
            default -> "English";
        };

        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("LANGUAGE_SELECT")
                .message("🌍 Language selected: " + langName)
                .status("INFO")
                .language(language)
                .build());
    }

    public void logPurposeSelected(String callSid, String purpose) {
        String purposeName = purpose.equals("employer") ? "Employer (Need Workers)" : "Job Seeker (Need Job)";

        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("PURPOSE_SELECT")
                .message("👤 Purpose: " + purposeName)
                .status("INFO")
                .purpose(purpose)
                .build());
    }

    public void logDataCollected(String callSid, String field, String value) {
        String emoji = switch (field) {
            case "name" -> "📝";
            case "work_expertise", "type_of_work" -> "🔧";
            case "location" -> "📍";
            default -> "ℹ️";
        };

        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("DATA_COLLECTED")
                .message(emoji + " " + field + ": " + value)
                .status("SUCCESS")
                .data("{\"" + field + "\": \"" + value + "\"}")
                .build());
    }

    public void logDatabaseSaved(String callSid, String type, Long id) {
        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("DB_SAVE")
                .message("💾 Saved to database (" + type + " #" + id + ")")
                .status("SUCCESS")
                .build());
    }

    public void logMatchingStarted(String callSid, int matchCount) {
        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("MATCHING")
                .message("🔍 Found " + matchCount + " matches")
                .status("SUCCESS")
                .build());
    }

    public void logSmsSent(String callSid, String phoneNo) {
        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("SMS_SENT")
                .message("📱 SMS sent to " + phoneNo)
                .status("SUCCESS")
                .phoneNo(phoneNo)
                .build());
    }

    public void logCallCompleted(String callSid, int durationSeconds) {
        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("CALL_COMPLETE")
                .message("✅ Call completed (Duration: " + durationSeconds + "s)")
                .status("SUCCESS")
                .build());
    }

    public void logError(String callSid, String errorMessage) {
        broadcastEvent(CallLogEvent.builder()
                .callSid(callSid)
                .eventType("ERROR")
                .message("❌ Error: " + errorMessage)
                .status("ERROR")
                .build());
    }
}