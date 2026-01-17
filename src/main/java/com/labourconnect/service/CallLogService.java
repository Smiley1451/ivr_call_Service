
package com.labourconnect.service;

import com.labourconnect.model.CallLog;
import com.labourconnect.repository.CallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for managing call logs
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CallLogService {

    private final CallLogRepository callLogRepository;

    /**
     * Logs a new call
     */
    @Transactional
    public CallLog logCall(String phoneNo, String callPurpose, String languageSelected,
                           Integer callDuration, String status) {
        log.info("Logging call for phone: {}, purpose: {}", phoneNo, callPurpose);

        CallLog callLog = CallLog.builder()
                .phoneNo(phoneNo)
                .callPurpose(callPurpose)
                .languageSelected(languageSelected)
                .callDuration(callDuration)
                .status(status)
                .build();

        CallLog saved = callLogRepository.save(callLog);
        log.info("Call logged with ID: {}", saved.getCallId());

        return saved;
    }

    /**
     * Gets recent calls
     */
    public List<CallLog> getRecentCalls() {
        log.info("Fetching recent calls");
        return callLogRepository.findTop20ByOrderByCallTimestampDesc();
    }

    /**
     * Gets calls by phone number
     */
    public List<CallLog> getCallsByPhoneNo(String phoneNo) {
        log.info("Fetching calls for phone: {}", phoneNo);
        return callLogRepository.findByPhoneNo(phoneNo);
    }

    /**
     * Gets call statistics
     */
    public CallStatistics getStatistics() {
        log.info("Generating call statistics");

        Long todayCount = callLogRepository.getTodayCallCount();
        Double avgDuration = callLogRepository.getAverageCallDuration();
        Double successRate = callLogRepository.getSuccessRate();

        List<Object[]> byPurpose = callLogRepository.countByPurpose();
        List<Object[]> byStatus = callLogRepository.countByStatus();
        List<Object[]> byLanguage = callLogRepository.countByLanguage();

        return new CallStatistics(todayCount, avgDuration, successRate,
                byPurpose, byStatus, byLanguage);
    }

    /**
     * Inner class for call statistics
     */
    public record CallStatistics(
            Long todayCallCount,
            Double averageCallDuration,
            Double successRate,
            List<Object[]> countByPurpose,
            List<Object[]> countByStatus,
            List<Object[]> countByLanguage
    ) {}
}