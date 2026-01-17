
// ============================================
// FILE: src/main/java/com/labourconnect/repository/WorkRepository.java
// ============================================
package com.labourconnect.repository;

import com.labourconnect.model.Work;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkRepository extends JpaRepository<Work, Long> {

    // Find by employer phone number
    List<Work> findByPhoneNo(String phoneNo);

    // Search by type of work (case-insensitive)
    List<Work> findByTypeOfWorkContainingIgnoreCase(String typeOfWork);

    // Search by location
    List<Work> findByLocationContainingIgnoreCase(String location);

    // Search by type and location
    @Query("SELECT w FROM Work w WHERE " +
            "LOWER(w.typeOfWork) LIKE LOWER(CONCAT('%', :type, '%')) " +
            "AND LOWER(w.location) LIKE LOWER(CONCAT('%', :location, '%')) " +
            "ORDER BY w.postedDate DESC")
    List<Work> findByTypeAndLocation(@Param("type") String type,
                                     @Param("location") String location);

    // Advanced search with weighted matching for job seekers
    @Query("SELECT w, " +
            "CASE " +
            "  WHEN LOWER(w.typeOfWork) LIKE LOWER(CONCAT('%', :skill, '%')) THEN 3 " +
            "  WHEN LOWER(w.description) LIKE LOWER(CONCAT('%', :skill, '%')) THEN 2 " +
            "  ELSE 1 " +
            "END as relevance, " +
            "CASE " +
            "  WHEN LOWER(w.location) = LOWER(:location) THEN 100 " +
            "  WHEN LOWER(w.location) LIKE LOWER(CONCAT('%', :location, '%')) THEN 50 " +
            "  ELSE 0 " +
            "END as locationScore " +
            "FROM Work w WHERE " +
            "LOWER(w.typeOfWork) LIKE LOWER(CONCAT('%', :skill, '%')) " +
            "OR LOWER(w.description) LIKE LOWER(CONCAT('%', :skill, '%')) " +
            "ORDER BY locationScore DESC, relevance DESC, w.postedDate DESC")
    List<Object[]> findMatchingJobsWithScore(@Param("skill") String skill,
                                             @Param("location") String location);

    // Find jobs by wage range
    @Query("SELECT w FROM Work w WHERE w.wagesOffered BETWEEN :minWage AND :maxWage")
    List<Work> findByWageRange(@Param("minWage") Integer minWage,
                               @Param("maxWage") Integer maxWage);

    // Find jobs offering minimum wage
    List<Work> findByWagesOfferedGreaterThanEqual(Integer minWage);

    // Count jobs by type
    @Query("SELECT w.typeOfWork, COUNT(w) FROM Work w GROUP BY w.typeOfWork")
    List<Object[]> countByType();

    // Get recent job postings
    List<Work> findTop10ByOrderByPostedDateDesc();

    // Find jobs by organisation
    List<Work> findByOrganisationNameContainingIgnoreCase(String organisationName);
}
