// ============================================
// FILE: src/main/java/com/labourconnect/dto/LabourDTO.java
// ============================================
package com.labourconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LabourDTO {
    private String phoneNo;
    private String name;
    private Integer experience;
    private String workExpertise;
    private String location;
    private Integer preferredWage;
    private String bio;
    private String languagePreference;
}
