package com.labourconnect.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "labour", indexes = {
        @Index(name = "idx_labour_expertise", columnList = "work_expertise"),
        @Index(name = "idx_labour_phone", columnList = "phone_no"),
        @Index(name = "idx_labour_location", columnList = "location")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Labour {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "labour_seq_gen")
    @SequenceGenerator(name = "labour_seq_gen", sequenceName = "labour_seq", initialValue = 100, allocationSize = 1)
    @Column(name = "labour_id")
    private Long labourId;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    @Column(name = "phone_no", nullable = false, length = 15)
    private String phoneNo;

    @Size(max = 100, message = "Name must be less than 100 characters")
    @Column(name = "name", length = 100)
    private String name;

    @Min(value = 0, message = "Experience cannot be negative")
    @Max(value = 50, message = "Experience cannot exceed 50 years")
    @Column(name = "experience")
    private Integer experience;

    @Size(max = 200, message = "Work expertise must be less than 200 characters")
    @Column(name = "work_expertise", length = 200)
    private String workExpertise;

    @Size(max = 100, message = "Location must be less than 100 characters")
    @Column(name = "location", length = 100)
    private String location;

    @Min(value = 0, message = "Preferred wage cannot be negative")
    @Column(name = "preferred_wage")
    private Integer preferredWage;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    @Pattern(regexp = "^(en|hi|kn)$", message = "Language must be en, hi, or kn")
    @Column(name = "language_preference", length = 10)
    private String languagePreference = "en";

    @CreationTimestamp
    @Column(name = "registration_date", nullable = false, updatable = false)
    private LocalDateTime registrationDate;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}