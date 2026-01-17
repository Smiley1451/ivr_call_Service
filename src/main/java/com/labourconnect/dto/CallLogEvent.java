// ============================================
// FILE: src/main/java/com/labourconnect/dto/CallLogEvent.java
// ============================================
package com.labourconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallLogEvent {
    private String callSid;
    private String phoneNo;
    private String eventType; // CALL_START, LANGUAGE_SELECT, etc.
    private String message;
    private String status; // INFO, SUCCESS, WARNING, ERROR
    private LocalDateTime timestamp;
    private String language;
    private String purpose;
    private String data; // Additional JSON data
}