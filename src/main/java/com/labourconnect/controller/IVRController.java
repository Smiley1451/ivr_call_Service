package com.labourconnect.controller;

import com.labourconnect.dto.*;
import com.labourconnect.model.Labour;
import com.labourconnect.model.Work;
import com.labourconnect.service.*;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for handling Twilio IVR webhooks with audio files and WebSocket logging
 */
@RestController
@RequestMapping("/ivr")
@Slf4j
@RequiredArgsConstructor
public class IVRController {

    private final LabourService labourService;
    private final WorkService workService;
    private final MatchingService matchingService;
    private final TwilioService twilioService;
    private final SpeechToTextService speechToTextService;
    private final CallLogService callLogService;
    private final AudioService audioService;
    private final WebSocketLogService webSocketLogService; // ✅ NEW

    @Value("${twilio.webhook.base.url}")
    private String baseUrl;

    @Value("${app.ivr.max.recording.duration:30}")
    private int maxRecordingDuration;

    @Value("${app.ivr.timeout.seconds:5}")
    private int timeout;

    private final Map<String, IVRSessionDTO> sessions = new ConcurrentHashMap<>();

    /**
     * STEP 1: Welcome message and language selection
     */
    @PostMapping(value = "/welcome", produces = MediaType.APPLICATION_XML_VALUE)
    public String welcome(@RequestParam("CallSid") String callSid,
                          @RequestParam("From") String fromPhoneNo) {
        log.info("New call received - CallSid: {}, From: {}", callSid, fromPhoneNo);

        // ✅ Broadcast to dashboard
        webSocketLogService.logCallStart(callSid, fromPhoneNo);

        IVRSessionDTO session = IVRSessionDTO.builder()
                .callSid(callSid)
                .phoneNo(fromPhoneNo)
                .currentStep(1)
                .startTime(System.currentTimeMillis())
                .build();
        sessions.put(callSid, session);

        String audioUrl = audioService.getAudioUrl("welcome", "en");
        Play play = new Play.Builder(audioUrl).build();

        Gather gather = new Gather.Builder()
                .numDigits(1)
                .timeout(timeout)
                .action(baseUrl + "/ivr/language")
                .play(play)
                .build();

        VoiceResponse response = new VoiceResponse.Builder()
                .gather(gather)
                .build();

        return response.toXml();
    }

    /**
     * STEP 2: Process language selection and ask purpose
     */
    @PostMapping(value = "/language", produces = MediaType.APPLICATION_XML_VALUE)
    public String selectLanguage(@RequestParam("CallSid") String callSid,
                                 @RequestParam("Digits") String digits) {
        log.info("Language selected - CallSid: {}, Digits: {}", callSid, digits);

        IVRSessionDTO session = sessions.get(callSid);
        if (session == null) {
            return createErrorResponse("Session expired. Please call again.");
        }

        String language = switch (digits) {
            case "2" -> "kn";
            case "3" -> "hi";
            default -> "en";
        };

        session.setLanguagePreference(language);
        session.setCurrentStep(2);

        // ✅ Broadcast to dashboard
        webSocketLogService.logLanguageSelected(callSid, language);

        String audioUrl = audioService.getAudioUrl("purpose_selection", language);
        Play play = new Play.Builder(audioUrl).build();

        Gather gather = new Gather.Builder()
                .numDigits(1)
                .timeout(timeout)
                .action(baseUrl + "/ivr/purpose")
                .play(play)
                .build();

        VoiceResponse response = new VoiceResponse.Builder()
                .gather(gather)
                .build();

        return response.toXml();
    }

    @PostMapping(value = "/purpose", produces = MediaType.APPLICATION_XML_VALUE)
    public String selectPurpose(@RequestParam("CallSid") String callSid,
                                @RequestParam("Digits") String digits) {
        log.info("Purpose selected - CallSid: {}, Digits: {}", callSid, digits);

        IVRSessionDTO session = sessions.get(callSid);
        if (session == null) {
            return createErrorResponse("Session expired. Please call again.");
        }

        String purpose = digits.equals("2") ? "employer" : "job_seeker";
        session.setCallPurpose(purpose);
        session.setCurrentStep(3);

        // ✅ Broadcast to dashboard
        webSocketLogService.logPurposeSelected(callSid, purpose);

        if (purpose.equals("job_seeker")) {
            return collectJobSeekerData(session, "name");
        } else {
            return collectEmployerData(session, "type_of_work");
        }
    }

    /**
     * Collects job seeker data
     */
    private String collectJobSeekerData(IVRSessionDTO session, String field) {
        String language = session.getLanguagePreference();
        String audioKey = "job_seeker_" + field;
        String audioUrl = audioService.getAudioUrl(audioKey, language);

        Play play = new Play.Builder(audioUrl).build();

        com.twilio.twiml.voice.Record record = new com.twilio.twiml.voice.Record.Builder()
                .maxLength(maxRecordingDuration)
                .timeout(timeout)
                .recordingStatusCallback(baseUrl + "/ivr/recording-status")
                .action(baseUrl + "/ivr/process-job-seeker/" + field)
                .build();

        VoiceResponse response = new VoiceResponse.Builder()
                .play(play)
                .record(record)
                .build();

        return response.toXml();
    }

    @PostMapping(value = "/process-job-seeker/{field}", produces = MediaType.APPLICATION_XML_VALUE)
    public String processJobSeekerResponse(@PathVariable String field,
                                           @RequestParam("CallSid") String callSid,
                                           @RequestParam("RecordingUrl") String recordingUrl) {
        log.info("Processing job seeker {} - CallSid: {}", field, callSid);

        IVRSessionDTO session = sessions.get(callSid);
        if (session == null) {
            return createErrorResponse("Session expired.");
        }

        // Store the recording URL for later processing
        session.addCollectedData(field + "_url", recordingUrl);
        log.info("Stored recording URL for {}: {}", field, recordingUrl);

        String nextField = switch (field) {
            case "name" -> "work_expertise";
            case "work_expertise" -> "location";
            case "location" -> null;
            default -> null;
        };

        if (nextField != null) {
            return collectJobSeekerData(session, nextField);
        } else {
            // All data collected, end call and start background processing
            return finalizeJobSeeker(session);
        }
    }

    private String finalizeJobSeeker(IVRSessionDTO session) {
        log.info("Finalizing job seeker registration - Starting background processing");

        // Start background processing
        processJobSeekerAsync(session);

        // Play completion message and hang up immediately
        String audioUrl = audioService.getAudioUrl("completion_job_seeker", session.getLanguagePreference());
        Play play = new Play.Builder(audioUrl).build();

        VoiceResponse response = new VoiceResponse.Builder()
                .play(play)
                .hangup(new Hangup.Builder().build())
                .build();

        return response.toXml();
    }

    @Async
    protected void processJobSeekerAsync(IVRSessionDTO session) {
        log.info("Async processing for job seeker started - CallSid: {}", session.getCallSid());
        
        try {
            // 1. Transcribe all audio files
            String nameUrl = session.getCollectedData().get("name_url");
            String expertiseUrl = session.getCollectedData().get("work_expertise_url");
            String locationUrl = session.getCollectedData().get("location_url");

            String name = transcribe(nameUrl, session.getLanguagePreference());
            String expertise = transcribe(expertiseUrl, session.getLanguagePreference());
            String location = transcribe(locationUrl, session.getLanguagePreference());

            // Update session with transcribed data
            session.addCollectedData("name", name);
            session.addCollectedData("work_expertise", expertise);
            session.addCollectedData("location", location);

            // Log collected data
            webSocketLogService.logDataCollected(session.getCallSid(), "name", name);
            webSocketLogService.logDataCollected(session.getCallSid(), "work_expertise", expertise);
            webSocketLogService.logDataCollected(session.getCallSid(), "location", location);

            // 2. Save to database
            LabourDTO labourDTO = LabourDTO.builder()
                    .phoneNo(session.getPhoneNo())
                    .name(name)
                    .workExpertise(expertise)
                    .location(location)
                    .languagePreference(session.getLanguagePreference())
                    .build();

            Labour labour = labourService.registerLabour(labourDTO);
            webSocketLogService.logDatabaseSaved(session.getCallSid(), "Labour", labour.getLabourId());

            // 3. Find matches
            MatchResultDTO matches = matchingService.findMatchingJobs(
                    labour.getWorkExpertise(),
                    labour.getLocation(),
                    labour.getPreferredWage()
            );

            int matchCount = matches.getJobs() != null ? matches.getJobs().size() : 0;
            webSocketLogService.logMatchingStarted(session.getCallSid(), matchCount);

            // 4. Send SMS
            twilioService.sendJobMatchesSMS(
                    session.getPhoneNo(),
                    matches.getJobs(),
                    session.getLanguagePreference()
            );
            webSocketLogService.logSmsSent(session.getCallSid(), session.getPhoneNo());

            // 5. Log call completion
            long duration = (System.currentTimeMillis() - session.getStartTime()) / 1000;
            callLogService.logCall(
                    session.getPhoneNo(),
                    "job_seeker",
                    session.getLanguagePreference(),
                    (int) duration,
                    "completed"
            );
            webSocketLogService.logCallCompleted(session.getCallSid(), (int) duration);

            // Clean up session
            sessions.remove(session.getCallSid());

        } catch (Exception e) {
            log.error("Error in async job seeker processing: {}", e.getMessage(), e);
            webSocketLogService.logError(session.getCallSid(), e.getMessage());
        }
    }

    /**
     * Collects employer data
     */
    private String collectEmployerData(IVRSessionDTO session, String field) {
        String language = session.getLanguagePreference();
        String audioKey = "employer_" + field;
        String audioUrl = audioService.getAudioUrl(audioKey, language);

        Play play = new Play.Builder(audioUrl).build();

        com.twilio.twiml.voice.Record record = new com.twilio.twiml.voice.Record.Builder()
                .maxLength(maxRecordingDuration)
                .timeout(timeout)
                .action(baseUrl + "/ivr/process-employer/" + field)
                .build();

        VoiceResponse response = new VoiceResponse.Builder()
                .play(play)
                .record(record)
                .build();

        return response.toXml();
    }

    @PostMapping(value = "/process-employer/{field}", produces = MediaType.APPLICATION_XML_VALUE)
    public String processEmployerResponse(@PathVariable String field,
                                          @RequestParam("CallSid") String callSid,
                                          @RequestParam("RecordingUrl") String recordingUrl) {
        log.info("Processing employer {} - CallSid: {}", field, callSid);

        IVRSessionDTO session = sessions.get(callSid);
        if (session == null) {
            return createErrorResponse("Session expired.");
        }

        // Store the recording URL for later processing
        session.addCollectedData(field + "_url", recordingUrl);
        log.info("Stored recording URL for {}: {}", field, recordingUrl);

        String nextField = switch (field) {
            case "type_of_work" -> "location";
            case "location" -> null;
            default -> null;
        };

        if (nextField != null) {
            return collectEmployerData(session, nextField);
        } else {
            // All data collected, end call and start background processing
            return finalizeEmployer(session);
        }
    }

    private String finalizeEmployer(IVRSessionDTO session) {
        log.info("Finalizing employer registration - Starting background processing");

        // Start background processing
        processEmployerAsync(session);

        // Play completion message and hang up immediately
        String audioUrl = audioService.getAudioUrl("completion_employer", session.getLanguagePreference());
        Play play = new Play.Builder(audioUrl).build();

        VoiceResponse response = new VoiceResponse.Builder()
                .play(play)
                .hangup(new Hangup.Builder().build())
                .build();

        return response.toXml();
    }

    @Async
    protected void processEmployerAsync(IVRSessionDTO session) {
        log.info("Async processing for employer started - CallSid: {}", session.getCallSid());

        try {
            // 1. Transcribe all audio files
            String typeUrl = session.getCollectedData().get("type_of_work_url");
            String locationUrl = session.getCollectedData().get("location_url");

            String typeOfWork = transcribe(typeUrl, session.getLanguagePreference());
            String location = transcribe(locationUrl, session.getLanguagePreference());

            // Update session with transcribed data
            session.addCollectedData("type_of_work", typeOfWork);
            session.addCollectedData("location", location);

            // Log collected data
            webSocketLogService.logDataCollected(session.getCallSid(), "type_of_work", typeOfWork);
            webSocketLogService.logDataCollected(session.getCallSid(), "location", location);

            // 2. Save to database
            WorkDTO workDTO = WorkDTO.builder()
                    .phoneNo(session.getPhoneNo())
                    .typeOfWork(typeOfWork)
                    .location(location)
                    .languagePreference(session.getLanguagePreference())
                    .build();

            Work work = workService.postWork(workDTO);
            webSocketLogService.logDatabaseSaved(session.getCallSid(), "Work", work.getWorkId());

            // 3. Find matches
            MatchResultDTO matches = matchingService.findMatchingWorkers(
                    work.getTypeOfWork(),
                    work.getLocation(),
                    work.getWagesOffered()
            );

            int matchCount = matches.getWorkers() != null ? matches.getWorkers().size() : 0;
            webSocketLogService.logMatchingStarted(session.getCallSid(), matchCount);

            // 4. Send SMS
            twilioService.sendWorkerMatchesSMS(
                    session.getPhoneNo(),
                    matches.getWorkers(),
                    session.getLanguagePreference()
            );
            webSocketLogService.logSmsSent(session.getCallSid(), session.getPhoneNo());

            // 5. Log call completion
            long duration = (System.currentTimeMillis() - session.getStartTime()) / 1000;
            callLogService.logCall(
                    session.getPhoneNo(),
                    "employer",
                    session.getLanguagePreference(),
                    (int) duration,
                    "completed"
            );
            webSocketLogService.logCallCompleted(session.getCallSid(), (int) duration);

            // Clean up session
            sessions.remove(session.getCallSid());

        } catch (Exception e) {
            log.error("Error in async employer processing: {}", e.getMessage(), e);
            webSocketLogService.logError(session.getCallSid(), e.getMessage());
        }
    }

    private String transcribe(String url, String language) {
        if (url == null) return "Unknown";
        
        String transcript = speechToTextService.transcribeAudioFromUrl(url + ".wav", language);
        
        if (transcript != null && speechToTextService.isValidTranscription(transcript)) {
            return speechToTextService.cleanTranscription(transcript);
        }
        
        return "Unknown";
    }

    @PostMapping("/recording-status")
    public void recordingStatus(@RequestParam Map<String, String> params) {
        log.info("Recording status: {}", params);
    }

    private String createErrorResponse(String message) {
        VoiceResponse response = new VoiceResponse.Builder()
                .say(new Say.Builder(message).build())
                .hangup(new Hangup.Builder().build())
                .build();

        return response.toXml();
    }
}