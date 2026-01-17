
// ============================================
// FILE: src/main/java/com/labourconnect/dto/SMSNotificationDTO.java
// ============================================
package com.labourconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for sending SMS notifications
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SMSNotificationDTO {
    private String toPhoneNo;
    private String message;
    private String language;
    private String messageType; // "job_matches" or "worker_matches"
}
