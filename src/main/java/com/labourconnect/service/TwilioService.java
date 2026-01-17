package com.labourconnect.service;

import com.labourconnect.dto.MatchResultDTO;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Service for Twilio SMS and Voice functionality
 */
@Service
@Slf4j
public class TwilioService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.phone.number}")
    private String fromPhoneNumber;

    @Value("${app.sms.max.matches:2}")
    private int maxMatches;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
        log.info("Twilio initialized with account SID: {}", accountSid);
    }

    public boolean sendSMS(String toPhoneNumber, String messageBody) {
        try {
            log.info("Sending SMS to: {}", toPhoneNumber);

            Message message = Message.creator(
                    new PhoneNumber(toPhoneNumber),
                    new PhoneNumber(fromPhoneNumber),
                    messageBody
            ).create();

            log.info("SMS sent successfully. SID: {}, Status: {}",
                    message.getSid(), message.getStatus());

            return true;

        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);
            return false;
        }
    }

    public boolean sendJobMatchesSMS(String toPhoneNumber,
                                     List<MatchResultDTO.JobMatch> jobs,
                                     String language) {
        if (jobs == null || jobs.isEmpty()) {
            String noMatchMessage = getNoMatchMessage(language, "jobs");
            return sendSMS(toPhoneNumber, noMatchMessage);
        }

        String message = formatJobMatchesSMS(jobs, language);
        return sendSMS(toPhoneNumber, message);
    }

    public boolean sendWorkerMatchesSMS(String toPhoneNumber,
                                        List<MatchResultDTO.WorkerMatch> workers,
                                        String language) {
        if (workers == null || workers.isEmpty()) {
            String noMatchMessage = getNoMatchMessage(language, "workers");
            return sendSMS(toPhoneNumber, noMatchMessage);
        }

        String message = formatWorkerMatchesSMS(workers, language);
        return sendSMS(toPhoneNumber, message);
    }

    /**
     * Formats job matches into compact SMS message
     */
    private String formatJobMatchesSMS(List<MatchResultDTO.JobMatch> jobs, String language) {
        StringBuilder sms = new StringBuilder();

        // Compact greeting and match count
        sms.append(getGreeting(language)).append(" ");
        sms.append(getMatchFoundMessage(language, Math.min(jobs.size(), maxMatches), "jobs")).append("\n");

        // Limit to max matches (default 2)
        int count = Math.min(jobs.size(), maxMatches);

        for (int i = 0; i < count; i++) {
            MatchResultDTO.JobMatch job = jobs.get(i);

            // Ultra compact format: 1. Role(Loc) Wage Ph:Number
            sms.append(i + 1).append(". ").append(job.getTypeOfWork());
            
            if (job.getLocation() != null) {
                sms.append("(").append(job.getLocation()).append(")");
            }

            if (job.getWagesOffered() != null) {
                sms.append(" ₹").append(job.getWagesOffered());
            }

            sms.append(" Ph:").append(job.getPhoneNo());

            // Add separator only if not last item
            if (i < count - 1) {
                sms.append("\n");
            }
        }

        return sms.toString();
    }

    /**
     * Formats worker matches into compact SMS message
     */
    private String formatWorkerMatchesSMS(List<MatchResultDTO.WorkerMatch> workers, String language) {
        StringBuilder sms = new StringBuilder();

        // Compact greeting and match count
        sms.append(getGreeting(language)).append(" ");
        sms.append(getMatchFoundMessage(language, Math.min(workers.size(), maxMatches), "workers")).append("\n");

        // Limit to max matches (default 2)
        int count = Math.min(workers.size(), maxMatches);

        for (int i = 0; i < count; i++) {
            MatchResultDTO.WorkerMatch worker = workers.get(i);

            // Ultra compact format: 1. Name - Skill(Loc) Ph:Number
            sms.append(i + 1).append(". ");
            sms.append(worker.getName() != null ? worker.getName() : "Worker");
            
            sms.append("-").append(worker.getExpertise());

            if (worker.getLocation() != null) {
                 sms.append("(").append(worker.getLocation()).append(")");
            }
            
            sms.append(" Ph:").append(worker.getPhoneNo());

            // Add separator only if not last item
            if (i < count - 1) {
                sms.append("\n");
            }
        }

        return sms.toString();
    }

    private String getGreeting(String language) {
        return switch (language != null ? language.toLowerCase() : "en") {
            case "hi" -> "नमस्ते!";
            case "kn" -> "ನಮಸ್ಕಾರ!";
            default -> "Hello!";
        };
    }

    private String getMatchFoundMessage(String language, int count, String type) {
        String jobsWord = type.equals("jobs") ?
                (language.equals("hi") ? "नौकरियां" : language.equals("kn") ? "ಉದ್ಯೋಗಗಳು" : "jobs") :
                (language.equals("hi") ? "कामगार" : language.equals("kn") ? "ಕಾರ್ಮಿಕರು" : "workers");

        return switch (language != null ? language.toLowerCase() : "en") {
            case "hi" -> String.format("%d %s मिले:", count, jobsWord);
            case "kn" -> String.format("%d %s ಸಿಕ್ಕಿತು:", count, jobsWord);
            default -> String.format("%d %s found:", count, type);
        };
    }

    private String getNoMatchMessage(String language, String type) {
        return switch (language != null ? language.toLowerCase() : "en") {
            case "hi" -> String.format("क्षमा करें, कोई %s नहीं मिली। बाद में पुनः प्रयास करें।",
                    type.equals("jobs") ? "नौकरी" : "कामगार");
            case "kn" -> String.format("ಕ್ಷಮಿಸಿ, %s ಸಿಗಲಿಲ್ಲ। ನಂತರ ಪ್ರಯತ್ನಿಸಿ.",
                    type.equals("jobs") ? "ಉದ್ಯೋಗ" : "ಕಾರ್ಮಿಕರು");
            default -> String.format("Sorry, no %s found. Try again later.", type);
        };
    }
}