// ============================================
// FILE: src/main/java/com/labourconnect/service/MatchingService.java
// ============================================
package com.labourconnect.service;

import com.labourconnect.dto.MatchResultDTO;
import com.labourconnect.model.Labour;
import com.labourconnect.model.Work;
import com.labourconnect.repository.LabourRepository;
import com.labourconnect.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for matching workers with jobs using weighted algorithm
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final LabourRepository labourRepository;
    private final WorkRepository workRepository;

    @Value("${app.matching.weight.location:0.4}")
    private double locationWeight;

    @Value("${app.matching.weight.experience:0.3}")
    private double experienceWeight;

    @Value("${app.matching.weight.skill:0.3}")
    private double skillWeight;

    @Value("${app.sms.max.matches:2}")
    private int maxMatches;

    /**
     * Finds matching jobs for a job seeker
     */
    public MatchResultDTO findMatchingJobs(String skill, String location, Integer preferredWage) {
        log.info("Finding jobs for skill: {}, location: {}, wage: {}", skill, location, preferredWage);

        try {
            // Get jobs with weighted scoring
            List<Object[]> results = workRepository.findMatchingJobsWithScore(skill, location);

            List<MatchResultDTO.JobMatch> jobMatches = new ArrayList<>();

            for (Object[] result : results) {
                Work work = (Work) result[0];
                Integer skillRelevance = (Integer) result[1];
                Integer locationScore = (Integer) result[2];

                // Calculate match score
                double matchScore = calculateJobMatchScore(
                        work,
                        skillRelevance,
                        locationScore,
                        preferredWage
                );

                // Create job match DTO
                MatchResultDTO.JobMatch jobMatch = MatchResultDTO.JobMatch.builder()
                        .workId(work.getWorkId())
                        .typeOfWork(work.getTypeOfWork())
                        .location(work.getLocation())
                        .wagesOffered(work.getWagesOffered())
                        .organisationName(work.getOrganisationName())
                        .phoneNo(work.getPhoneNo())
                        .matchScore(matchScore)
                        .build();

                jobMatches.add(jobMatch);
            }

            // Sort by match score and limit results
            List<MatchResultDTO.JobMatch> topMatches = jobMatches.stream()
                    .sorted((j1, j2) -> Double.compare(j2.getMatchScore(), j1.getMatchScore()))
                    .limit(maxMatches)
                    .collect(Collectors.toList());

            log.info("Found {} matching jobs", topMatches.size());

            return MatchResultDTO.builder()
                    .searchType("jobs")
                    .totalMatches(topMatches.size())
                    .jobs(topMatches)
                    .build();

        } catch (Exception e) {
            log.error("Error finding matching jobs: {}", e.getMessage(), e);
            return MatchResultDTO.builder()
                    .searchType("jobs")
                    .totalMatches(0)
                    .jobs(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Finds matching workers for an employer
     */
    public MatchResultDTO findMatchingWorkers(String requiredSkill, String location, Integer offeredWage) {
        log.info("Finding workers for skill: {}, location: {}, wage: {}", requiredSkill, location, offeredWage);

        try {
            // Get workers with weighted scoring
            List<Object[]> results = labourRepository.findMatchingWorkersWithScore(requiredSkill, location);

            List<MatchResultDTO.WorkerMatch> workerMatches = new ArrayList<>();

            for (Object[] result : results) {
                Labour labour = (Labour) result[0];
                Integer skillRelevance = (Integer) result[1];
                Integer locationScore = (Integer) result[2];

                // Calculate match score
                double matchScore = calculateWorkerMatchScore(
                        labour,
                        skillRelevance,
                        locationScore,
                        offeredWage
                );

                // Create worker match DTO
                MatchResultDTO.WorkerMatch workerMatch = MatchResultDTO.WorkerMatch.builder()
                        .labourId(labour.getLabourId())
                        .name(labour.getName())
                        .expertise(labour.getWorkExpertise())
                        .experience(labour.getExperience())
                        .location(labour.getLocation())
                        .phoneNo(labour.getPhoneNo())
                        .preferredWage(labour.getPreferredWage())
                        .matchScore(matchScore)
                        .build();

                workerMatches.add(workerMatch);
            }

            // Sort by match score and limit results
            List<MatchResultDTO.WorkerMatch> topMatches = workerMatches.stream()
                    .sorted((w1, w2) -> Double.compare(w2.getMatchScore(), w1.getMatchScore()))
                    .limit(maxMatches)
                    .collect(Collectors.toList());

            log.info("Found {} matching workers", topMatches.size());

            return MatchResultDTO.builder()
                    .searchType("workers")
                    .totalMatches(topMatches.size())
                    .workers(topMatches)
                    .build();

        } catch (Exception e) {
            log.error("Error finding matching workers: {}", e.getMessage(), e);
            return MatchResultDTO.builder()
                    .searchType("workers")
                    .totalMatches(0)
                    .workers(new ArrayList<>())
                    .build();
        }
    }

    /**
     * Calculates match score for a job
     * Score range: 0-100
     */
    private double calculateJobMatchScore(Work work,
                                          Integer skillRelevance,
                                          Integer locationScore,
                                          Integer preferredWage) {
        double score = 0.0;

        // Skill matching (30%)
        score += (skillRelevance / 3.0) * 100 * skillWeight;

        // Location matching (40%)
        score += locationScore * locationWeight;

        // Wage matching (30%)
        if (preferredWage != null && work.getWagesOffered() != null) {
            double wageScore = 0.0;
            if (work.getWagesOffered() >= preferredWage) {
                wageScore = 100.0;
            } else {
                // Proportional score if wage is lower
                wageScore = (work.getWagesOffered().doubleValue() / preferredWage) * 100;
            }
            score += wageScore * experienceWeight;
        } else {
            score += 50 * experienceWeight; // Neutral score if wage not specified
        }

        return Math.min(score, 100.0);
    }

    /**
     * Calculates match score for a worker
     * Score range: 0-100
     */
    private double calculateWorkerMatchScore(Labour labour,
                                             Integer skillRelevance,
                                             Integer locationScore,
                                             Integer offeredWage) {
        double score = 0.0;

        // Skill matching (30%)
        score += (skillRelevance / 3.0) * 100 * skillWeight;

        // Location matching (40%)
        score += locationScore * locationWeight;

        // Experience matching (30%)
        if (labour.getExperience() != null) {
            // More experience = higher score (max 10 years = 100%)
            double experienceScore = Math.min((labour.getExperience() / 10.0) * 100, 100.0);
            score += experienceScore * experienceWeight;
        } else {
            score += 30 * experienceWeight; // Default score for no experience data
        }

        // Bonus: Wage compatibility (not part of main score, but reduces score if mismatch)
        if (offeredWage != null && labour.getPreferredWage() != null) {
            if (labour.getPreferredWage() > offeredWage * 1.2) {
                // Reduce score if worker expects significantly more
                score *= 0.9;
            }
        }

        return Math.min(score, 100.0);
    }

    /**
     * Finds all available workers by skill
     */
    public List<Labour> findWorkersBySkill(String skill) {
        log.info("Finding workers by skill: {}", skill);
        return labourRepository.findByWorkExpertiseContainingIgnoreCase(skill);
    }

    /**
     * Finds all available jobs by type
     */
    public List<Work> findJobsByType(String type) {
        log.info("Finding jobs by type: {}", type);
        return workRepository.findByTypeOfWorkContainingIgnoreCase(type);
    }
}