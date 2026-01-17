

// ============================================
// FILE: src/main/java/com/labourconnect/dto/IVRSessionDTO.java
// ============================================
package com.labourconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores IVR session data temporarily during call flow
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IVRSessionDTO {
    private String callSid;
    private String phoneNo;
    private String languagePreference;
    private String callPurpose; // job_seeker or employer
    private Map<String, String> collectedData = new HashMap<>();
    private Integer currentStep;
    private Long startTime;

    public void addCollectedData(String key, String value) {
        if (collectedData == null) {
            collectedData = new HashMap<>();
        }
        collectedData.put(key, value);
    }
}
