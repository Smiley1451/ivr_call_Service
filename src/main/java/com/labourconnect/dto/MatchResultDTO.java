
// ============================================
// FILE: src/main/java/com/labourconnect/dto/MatchResultDTO.java
// ============================================
package com.labourconnect.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Response containing matched workers or jobs
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResultDTO {
    private String searchType; // "workers" or "jobs"
    private Integer totalMatches;
    private List<WorkerMatch> workers;
    private List<JobMatch> jobs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkerMatch {
        private Long labourId;
        private String name;
        private String expertise;
        private Integer experience;
        private String location;
        private String phoneNo;
        private Integer preferredWage;
        private Double matchScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class JobMatch {
        private Long workId;
        private String typeOfWork;
        private String location;
        private Integer wagesOffered;
        private String organisationName;
        private String phoneNo;
        private Double matchScore;
    }
}
