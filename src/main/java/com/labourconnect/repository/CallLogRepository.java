// ============================================
// FILE: src/main/java/com/labourconnect/repository/CallLogRepository.java
// ============================================
package com.labourconnect.repository;

import com.labourconnect.model.CallLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CallLogRepository extends JpaRepository<CallLog, Long> {

    // Find calls by phone number
    List<CallLog> findByPhoneNo(String phoneNo);

    // Find calls by purpose
    List<CallLog> findByCallPurpose(String callPurpose);

    // Find calls by status
    List<CallLog> findByStatus(String status);

    // Find recent calls
    List<CallLog> findTop20ByOrderByCallTimestampDesc();

    // Find calls by phone number and purpose
    List<CallLog> findByPhoneNoAndCallPurpose(String phoneNo, String callPurpose);

    // Find calls within time range
    @Query("SELECT c FROM CallLog c WHERE c.callTimestamp BETWEEN :startTime AND :endTime")
    List<CallLog> findByTimestampRange(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    // Count calls by purpose
    @Query("SELECT c.callPurpose, COUNT(c) FROM CallLog c GROUP BY c.callPurpose")
    List<Object[]> countByPurpose();

    // Count calls by status
    @Query("SELECT c.status, COUNT(c) FROM CallLog c GROUP BY c.status")
    List<Object[]> countByStatus();

    // Count calls by language
    @Query("SELECT c.languageSelected, COUNT(c) FROM CallLog c GROUP BY c.languageSelected")
    List<Object[]> countByLanguage();

    // Get average call duration (FIXED)
    @Query("SELECT AVG(c.callDuration) FROM CallLog c WHERE c.status = 'completed'")
    Double getAverageCallDuration();

    // Get total calls today (FIXED - Using JPQL compatible syntax)
    @Query("SELECT COUNT(c) FROM CallLog c WHERE FUNCTION('DATE', c.callTimestamp) = CURRENT_DATE")
    Long getTodayCallCount();

    // Get success rate (FIXED)
    @Query("SELECT CAST(SUM(CASE WHEN c.status = 'completed' THEN 1.0 ELSE 0.0 END) * 100.0 / COUNT(c) AS double) FROM CallLog c")
    Double getSuccessRate();
}