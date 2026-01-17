// ============================================
// FILE: src/main/java/com/labourconnect/repository/LabourRepository.java
// ============================================
package com.labourconnect.repository;

import com.labourconnect.model.Labour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LabourRepository extends JpaRepository<Labour, Long> {

    // Find by phone number
    Optional<Labour> findByPhoneNo(String phoneNo);

    // Find all workers with specific phone number (for re-registration tracking)
    List<Labour> findAllByPhoneNo(String phoneNo);

    // Search by work expertise (case-insensitive)
    List<Labour> findByWorkExpertiseContainingIgnoreCase(String expertise);

    // Search by location
    List<Labour> findByLocationContainingIgnoreCase(String location);

    // Search by expertise and location with experience ordering
    @Query("SELECT l FROM Labour l WHERE " +
            "LOWER(l.workExpertise) LIKE LOWER(CONCAT('%', :skill, '%')) " +
            "AND LOWER(l.location) LIKE LOWER(CONCAT('%', :location, '%')) " +
            "ORDER BY l.experience DESC, l.lastUpdated DESC")
    List<Labour> findBySkillAndLocation(@Param("skill") String skill,
                                        @Param("location") String location);

    // Advanced search with weighted matching
    @Query("SELECT l, " +
            "CASE " +
            "  WHEN LOWER(l.workExpertise) LIKE LOWER(CONCAT('%', :skill, '%')) THEN 3 " +
            "  WHEN LOWER(l.bio) LIKE LOWER(CONCAT('%', :skill, '%')) THEN 2 " +
            "  ELSE 1 " +
            "END as relevance, " +
            "CASE " +
            "  WHEN LOWER(l.location) = LOWER(:location) THEN 100 " +
            "  WHEN LOWER(l.location) LIKE LOWER(CONCAT('%', :location, '%')) THEN 50 " +
            "  ELSE 0 " +
            "END as locationScore " +
            "FROM Labour l WHERE " +
            "LOWER(l.workExpertise) LIKE LOWER(CONCAT('%', :skill, '%')) " +
            "OR LOWER(l.bio) LIKE LOWER(CONCAT('%', :skill, '%')) " +
            "ORDER BY locationScore DESC, relevance DESC, l.experience DESC")
    List<Object[]> findMatchingWorkersWithScore(@Param("skill") String skill,
                                                @Param("location") String location);

    // Find workers by minimum experience
    List<Labour> findByExperienceGreaterThanEqual(Integer minExperience);

    // Find workers by wage range
    @Query("SELECT l FROM Labour l WHERE l.preferredWage BETWEEN :minWage AND :maxWage")
    List<Labour> findByWageRange(@Param("minWage") Integer minWage,
                                 @Param("maxWage") Integer maxWage);

    // Count workers by expertise
    @Query("SELECT l.workExpertise, COUNT(l) FROM Labour l GROUP BY l.workExpertise")
    List<Object[]> countByExpertise();

    // Get recent registrations
    List<Labour> findTop10ByOrderByRegistrationDateDesc();
}
