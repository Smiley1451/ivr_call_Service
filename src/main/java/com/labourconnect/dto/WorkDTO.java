
// ============================================
// FILE: src/main/java/com/labourconnect/dto/WorkDTO.java
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
public class WorkDTO {
    private String phoneNo;
    private String typeOfWork;
    private String location;
    private Integer wagesOffered;
    private String organisationName;
    private String description;
    private String languagePreference;
}