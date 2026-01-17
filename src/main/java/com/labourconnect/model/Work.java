

// ============================================
// FILE: src/main/java/com/labourconnect/model/Work.java
// ============================================
package com.labourconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "work", indexes = {
        @Index(name = "idx_work_type", columnList = "type_of_work"),
        @Index(name = "idx_work_location", columnList = "location"),
        @Index(name = "idx_work_phone", columnList = "phone_no")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "work_seq_gen")
    @SequenceGenerator(name = "work_seq_gen", sequenceName = "work_seq", initialValue = 100, allocationSize = 1)
    @Column(name = "work_id")
    private Long workId;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @NotBlank(message = "Type of work is required")
    @Size(max = 150, message = "Type of work must be less than 150 characters")
    @Column(name = "type_of_work", nullable = false, length = 150)
    private String typeOfWork;

    @NotBlank(message = "Location is required")
    @Size(max = 100, message = "Location must be less than 100 characters")
    @Column(name = "location", nullable = false, length = 100)
    private String location;

    @Min(value = 0, message = "Wages cannot be negative")
    @Column(name = "wages_offered")
    private Integer wagesOffered;

    @Size(max = 150, message = "Organisation name must be less than 150 characters")
    @Column(name = "organisation_name", length = 150)
    private String organisationName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Pattern(regexp = "^(en|hi|kn)$", message = "Language must be en, hi, or kn")
    @Column(name = "language_preference", length = 10)
    private String languagePreference = "en";

    @CreationTimestamp
    @Column(name = "posted_date", nullable = false, updatable = false)
    private LocalDateTime postedDate;
}
