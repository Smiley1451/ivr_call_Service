// ============================================
// FILE: src/main/java/com/labourconnect/service/LabourService.java
// ============================================
package com.labourconnect.service;

import com.labourconnect.dto.LabourDTO;
import com.labourconnect.model.Labour;
import com.labourconnect.repository.LabourRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing labour/worker data
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LabourService {

    private final LabourRepository labourRepository;

    /**
     * Registers a new worker
     */
    @Transactional
    public Labour registerLabour(LabourDTO labourDTO) {
        log.info("Registering new worker with phone: {}", labourDTO.getPhoneNo());

        Labour labour = Labour.builder()
                .phoneNo(labourDTO.getPhoneNo())
                .name(labourDTO.getName())
                .experience(labourDTO.getExperience())
                .workExpertise(labourDTO.getWorkExpertise())
                .location(labourDTO.getLocation())
                .preferredWage(labourDTO.getPreferredWage())
                .bio(labourDTO.getBio())
                .languagePreference(labourDTO.getLanguagePreference())
                .build();

        Labour saved = labourRepository.save(labour);
        log.info("Worker registered successfully with ID: {}", saved.getLabourId());

        return saved;
    }

    /**
     * Updates existing worker information
     */
    @Transactional
    public Labour updateLabour(Long labourId, LabourDTO labourDTO) {
        log.info("Updating worker with ID: {}", labourId);

        Optional<Labour> existing = labourRepository.findById(labourId);

        if (existing.isEmpty()) {
            log.warn("Worker not found with ID: {}", labourId);
            return null;
        }

        Labour labour = existing.get();

        // Update fields
        if (labourDTO.getName() != null) labour.setName(labourDTO.getName());
        if (labourDTO.getExperience() != null) labour.setExperience(labourDTO.getExperience());
        if (labourDTO.getWorkExpertise() != null) labour.setWorkExpertise(labourDTO.getWorkExpertise());
        if (labourDTO.getLocation() != null) labour.setLocation(labourDTO.getLocation());
        if (labourDTO.getPreferredWage() != null) labour.setPreferredWage(labourDTO.getPreferredWage());
        if (labourDTO.getBio() != null) labour.setBio(labourDTO.getBio());
        if (labourDTO.getLanguagePreference() != null) labour.setLanguagePreference(labourDTO.getLanguagePreference());

        Labour updated = labourRepository.save(labour);
        log.info("Worker updated successfully");

        return updated;
    }

    /**
     * Finds worker by phone number (most recent registration)
     */
    public Labour findByPhoneNo(String phoneNo) {
        log.info("Finding worker by phone: {}", phoneNo);
        return labourRepository.findByPhoneNo(phoneNo).orElse(null);
    }

    /**
     * Finds all registrations for a phone number
     */
    public List<Labour> findAllByPhoneNo(String phoneNo) {
        log.info("Finding all registrations for phone: {}", phoneNo);
        return labourRepository.findAllByPhoneNo(phoneNo);
    }

    /**
     * Gets all workers
     */
    public List<Labour> getAllLabour() {
        log.info("Fetching all workers");
        return labourRepository.findAll();
    }

    /**
     * Gets recent registrations
     */
    public List<Labour> getRecentRegistrations() {
        log.info("Fetching recent registrations");
        return labourRepository.findTop10ByOrderByRegistrationDateDesc();
    }

    /**
     * Searches workers by expertise
     */
    public List<Labour> searchByExpertise(String expertise) {
        log.info("Searching workers by expertise: {}", expertise);
        return labourRepository.findByWorkExpertiseContainingIgnoreCase(expertise);
    }

    /**
     * Searches workers by location
     */
    public List<Labour> searchByLocation(String location) {
        log.info("Searching workers by location: {}", location);
        return labourRepository.findByLocationContainingIgnoreCase(location);
    }

    /**
     * Gets worker count by expertise
     */
    public List<Object[]> getWorkerCountByExpertise() {
        log.info("Getting worker count by expertise");
        return labourRepository.countByExpertise();
    }

    /**
     * Deletes a worker
     */
    @Transactional
    public boolean deleteLabour(Long labourId) {
        log.info("Deleting worker with ID: {}", labourId);

        if (labourRepository.existsById(labourId)) {
            labourRepository.deleteById(labourId);
            log.info("Worker deleted successfully");
            return true;
        }

        log.warn("Worker not found with ID: {}", labourId);
        return false;
    }
}