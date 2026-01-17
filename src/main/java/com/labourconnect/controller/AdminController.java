package com.labourconnect.controller;

import com.labourconnect.model.Labour;
import com.labourconnect.model.Work;
import com.labourconnect.model.CallLog;
import com.labourconnect.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for viewing data and statistics
 */
@RestController
@RequestMapping("/api/admin")
@Slf4j
@RequiredArgsConstructor
public class AdminController {

    private final LabourService labourService;
    private final WorkService workService;
    private final CallLogService callLogService;

    /**
     * Get all workers
     */
    @GetMapping("/workers")
    public ResponseEntity<List<Labour>> getAllWorkers() {
        log.info("Fetching all workers");
        return ResponseEntity.ok(labourService.getAllLabour());
    }

    /**
     * Get all jobs
     */
    @GetMapping("/jobs")
    public ResponseEntity<List<Work>> getAllJobs() {
        log.info("Fetching all jobs");
        return ResponseEntity.ok(workService.getAllWork());
    }

    /**
     * Get recent call logs
     */
    @GetMapping("/calls")
    public ResponseEntity<List<CallLog>> getRecentCalls() {
        log.info("Fetching recent calls");
        return ResponseEntity.ok(callLogService.getRecentCalls());
    }

    /**
     * Get dashboard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        log.info("Generating dashboard statistics");

        Map<String, Object> stats = new HashMap<>();

        // Worker statistics
        List<Labour> workers = labourService.getAllLabour();
        stats.put("totalWorkers", workers.size());
        stats.put("workersByExpertise", labourService.getWorkerCountByExpertise());
        stats.put("recentWorkers", labourService.getRecentRegistrations());

        // Job statistics
        List<Work> jobs = workService.getAllWork();
        stats.put("totalJobs", jobs.size());
        stats.put("jobsByType", workService.getJobCountByType());
        stats.put("recentJobs", workService.getRecentPostings());

        // Call statistics
        CallLogService.CallStatistics callStats = callLogService.getStatistics();
        stats.put("todayCallCount", callStats.todayCallCount());
        stats.put("averageCallDuration", callStats.averageCallDuration());
        stats.put("successRate", callStats.successRate());
        stats.put("callsByPurpose", callStats.countByPurpose());
        stats.put("callsByStatus", callStats.countByStatus());
        stats.put("callsByLanguage", callStats.countByLanguage());

        return ResponseEntity.ok(stats);
    }

    /**
     * Search workers by expertise
     */
    @GetMapping("/workers/search")
    public ResponseEntity<List<Labour>> searchWorkers(@RequestParam String expertise) {
        log.info("Searching workers by expertise: {}", expertise);
        return ResponseEntity.ok(labourService.searchByExpertise(expertise));
    }

    /**
     * Search jobs by type
     */
    @GetMapping("/jobs/search")
    public ResponseEntity<List<Work>> searchJobs(@RequestParam String type) {
        log.info("Searching jobs by type: {}", type);
        return ResponseEntity.ok(workService.searchByType(type));
    }

    /**
     * Delete a worker
     */
    @DeleteMapping("/workers/{id}")
    public ResponseEntity<Map<String, String>> deleteWorker(@PathVariable Long id) {
        log.info("Deleting worker with ID: {}", id);

        boolean deleted = labourService.deleteLabour(id);

        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Worker deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Delete a job
     */
    @DeleteMapping("/jobs/{id}")
    public ResponseEntity<Map<String, String>> deleteJob(@PathVariable Long id) {
        log.info("Deleting job with ID: {}", id);

        boolean deleted = workService.deleteWork(id);

        if (deleted) {
            return ResponseEntity.ok(Map.of("message", "Job deleted successfully"));
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}