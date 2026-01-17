

// ============================================
// FILE: src/main/java/com/labourconnect/service/WorkService.java
// ============================================
package com.labourconnect.service;

import com.labourconnect.dto.WorkDTO;
import com.labourconnect.model.Work;
import com.labourconnect.repository.WorkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing work/job postings
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WorkService {

    private final WorkRepository workRepository;

    /**
     * Posts a new job
     */
    @Transactional
    public Work postWork(WorkDTO workDTO) {
        log.info("Posting new job from phone: {}", workDTO.getPhoneNo());

        Work work = Work.builder()
                .phoneNo(workDTO.getPhoneNo())
                .typeOfWork(workDTO.getTypeOfWork())
                .location(workDTO.getLocation())
                .wagesOffered(workDTO.getWagesOffered())
                .organisationName(workDTO.getOrganisationName())
                .description(workDTO.getDescription())
                .languagePreference(workDTO.getLanguagePreference())
                .build();

        Work saved = workRepository.save(work);
        log.info("Job posted successfully with ID: {}", saved.getWorkId());

        return saved;
    }

    /**
     * Updates existing job posting
     */
    @Transactional
    public Work updateWork(Long workId, WorkDTO workDTO) {
        log.info("Updating job with ID: {}", workId);

        Optional<Work> existing = workRepository.findById(workId);

        if (existing.isEmpty()) {
            log.warn("Job not found with ID: {}", workId);
            return null;
        }

        Work work = existing.get();

        // Update fields
        if (workDTO.getTypeOfWork() != null) work.setTypeOfWork(workDTO.getTypeOfWork());
        if (workDTO.getLocation() != null) work.setLocation(workDTO.getLocation());
        if (workDTO.getWagesOffered() != null) work.setWagesOffered(workDTO.getWagesOffered());
        if (workDTO.getOrganisationName() != null) work.setOrganisationName(workDTO.getOrganisationName());
        if (workDTO.getDescription() != null) work.setDescription(workDTO.getDescription());
        if (workDTO.getLanguagePreference() != null) work.setLanguagePreference(workDTO.getLanguagePreference());

        Work updated = workRepository.save(work);
        log.info("Job updated successfully");

        return updated;
    }

    /**
     * Finds jobs posted by phone number
     */
    public List<Work> findByPhoneNo(String phoneNo) {
        log.info("Finding jobs posted by phone: {}", phoneNo);
        return workRepository.findByPhoneNo(phoneNo);
    }

    /**
     * Gets all job postings
     */
    public List<Work> getAllWork() {
        log.info("Fetching all jobs");
        return workRepository.findAll();
    }

    /**
     * Gets recent job postings
     */
    public List<Work> getRecentPostings() {
        log.info("Fetching recent job postings");
        return workRepository.findTop10ByOrderByPostedDateDesc();
    }

    /**
     * Searches jobs by type
     */
    public List<Work> searchByType(String type) {
        log.info("Searching jobs by type: {}", type);
        return workRepository.findByTypeOfWorkContainingIgnoreCase(type);
    }

    /**
     * Searches jobs by location
     */
    public List<Work> searchByLocation(String location) {
        log.info("Searching jobs by location: {}", location);
        return workRepository.findByLocationContainingIgnoreCase(location);
    }

    /**
     * Gets job count by type
     */
    public List<Object[]> getJobCountByType() {
        log.info("Getting job count by type");
        return workRepository.countByType();
    }

    /**
     * Deletes a job posting
     */
    @Transactional
    public boolean deleteWork(Long workId) {
        log.info("Deleting job with ID: {}", workId);

        if (workRepository.existsById(workId)) {
            workRepository.deleteById(workId);
            log.info("Job deleted successfully");
            return true;
        }

        log.warn("Job not found with ID: {}", workId);
        return false;
    }
}

// ====================