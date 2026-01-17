
// ============================================
// FILE: src/main/java/com/labourconnect/model/CallLog.java
// ============================================
package com.labourconnect.model;

import jakarta.persistence.*;
        import jakarta.validation.constraints.*;
        import lombok.*;
        import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "call_logs", indexes = {
        @Index(name = "idx_call_logs_timestamp", columnList = "call_timestamp"),
        @Index(name = "idx_call_logs_phone", columnList = "phone_no")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CallLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_id")
    private Long callId;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_no", length = 15)
    private String phoneNo;

    @Pattern(regexp = "^(job_seeker|employer)$", message = "Call purpose must be job_seeker or employer")
    @Column(name = "call_purpose", length = 50)
    private String callPurpose;

    @Pattern(regexp = "^(en|hi|kn)$", message = "Language must be en, hi, or kn")
    @Column(name = "language_selected", length = 10)
    private String languageSelected;

    @Min(value = 0, message = "Call duration cannot be negative")
    @Column(name = "call_duration")
    private Integer callDuration; // in seconds

    @CreationTimestamp
    @Column(name = "call_timestamp", nullable = false, updatable = false)
    private LocalDateTime callTimestamp;

    @Pattern(regexp = "^(completed|dropped|failed)$", message = "Status must be completed, dropped, or failed")
    @Column(name = "status", length = 20)
    private String status = "completed";
}