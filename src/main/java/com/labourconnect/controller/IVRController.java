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

        String transcript = speechToTextService.transcribeAudioFromUrl(
                recordingUrl + ".wav",
                session.getLanguagePreference()
        );

        if (transcript != null && speechToTextService.isValidTranscription(transcript)) {
            transcript = speechToTextService.cleanTranscription(transcript);
            session.addCollectedData(field, transcript);
            log.info("Collected {}: {}", field, transcript);

            // ✅ Broadcast to dashboard
            webSocketLogService.logDataCollected(callSid, field, transcript);
        }

        String nextField = switch (field) {
            case "name" -> "work_expertise";
            case "work_expertise" -> "location";
            case "location" -> null;
            default -> null;
        };

        if (nextField != null) {
            return collectJobSeekerData(session, nextField);
        } else {
            return finalizeJobSeeker(session);
        }
    }

    private String finalizeJobSeeker(IVRSessionDTO session) {
        log.info("Finalizing job seeker registration");

        try {
            LabourDTO labourDTO = LabourDTO.builder()
                    .phoneNo(session.getPhoneNo())
                    .name(session.getCollectedData().get("name"))
                    .workExpertise(session.getCollectedData().get("work_expertise"))
                    .location(session.getCollectedData().get("location"))
                    .languagePreference(session.getLanguagePreference())
                    .build();

            Labour labour = labourService.registerLabour(labourDTO);

            // ✅ Broadcast to dashboard
            webSocketLogService.logDatabaseSaved(session.getCallSid(), "Labour", labour.getLabourId());

            MatchResultDTO matches = matchingService.findMatchingJobs(
                    labour.getWorkExpertise(),
                    labour.getLocation(),
                    labour.getPreferredWage()
            );

            // ✅ Broadcast to dashboard
            int matchCount = matches.getJobs() != null ? matches.getJobs().size() : 0;
            webSocketLogService.logMatchingStarted(session.getCallSid(), matchCount);

            twilioService.sendJobMatchesSMS(
                    session.getPhoneNo(),
                    matches.getJobs(),
                    session.getLanguagePreference()
            );

            // ✅ Broadcast to dashboard
            webSocketLogService.logSmsSent(session.getCallSid(), session.getPhoneNo());

            long duration = (System.currentTimeMillis() - session.getStartTime()) / 1000;
            callLogService.logCall(
                    session.getPhoneNo(),
                    "job_seeker",
                    session.getLanguagePreference(),
                    (int) duration,
                    "completed"
            );

            // ✅ Broadcast to dashboard
            webSocketLogService.logCallCompleted(session.getCallSid(), (int) duration);

            sessions.remove(session.getCallSid());

            String audioUrl = audioService.getAudioUrl("completion_job_seeker", session.getLanguagePreference());
            Play play = new Play.Builder(audioUrl).build();

            VoiceResponse response = new VoiceResponse.Builder()
                    .play(play)
                    .hangup(new Hangup.Builder().build())
                    .build();

            return response.toXml();

        } catch (Exception e) {
            log.error("Error finalizing job seeker: {}", e.getMessage(), e);

            // ✅ Broadcast error to dashboard
            webSocketLogService.logError(session.getCallSid(), e.getMessage());

            return createErrorResponse("An error occurred. Please try again.");
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

        String transcript = speechToTextService.transcribeAudioFromUrl(
                recordingUrl + ".wav",
                session.getLanguagePreference()
        );

        if (transcript != null && speechToTextService.isValidTranscription(transcript)) {
            transcript = speechToTextService.cleanTranscription(transcript);
            session.addCollectedData(field, transcript);
            log.info("Collected {}: {}", field, transcript);

            // ✅ Broadcast to dashboard
            webSocketLogService.logDataCollected(callSid, field, transcript);
        }

        String nextField = switch (field) {
            case "type_of_work" -> "location";
            case "location" -> null;
            default -> null;
        };

        if (nextField != null) {
            return collectEmployerData(session, nextField);
        } else {
            return finalizeEmployer(session);
        }
    }

    private String finalizeEmployer(IVRSessionDTO session) {
        log.info("Finalizing employer registration");

        try {
            WorkDTO workDTO = WorkDTO.builder()
                    .phoneNo(session.getPhoneNo())
                    .typeOfWork(session.getCollectedData().get("type_of_work"))
                    .location(session.getCollectedData().get("location"))
                    .languagePreference(session.getLanguagePreference())
                    .build();

            Work work = workService.postWork(workDTO);

            // ✅ Broadcast to dashboard
            webSocketLogService.logDatabaseSaved(session.getCallSid(), "Work", work.getWorkId());

            MatchResultDTO matches = matchingService.findMatchingWorkers(
                    work.getTypeOfWork(),
                    work.getLocation(),
                    work.getWagesOffered()
            );

            // ✅ Broadcast to dashboard
            int matchCount = matches.getWorkers() != null ? matches.getWorkers().size() : 0;
            webSocketLogService.logMatchingStarted(session.getCallSid(), matchCount);

            twilioService.sendWorkerMatchesSMS(
                    session.getPhoneNo(),
                    matches.getWorkers(),
                    session.getLanguagePreference()
            );

            // ✅ Broadcast to dashboard
            webSocketLogService.logSmsSent(session.getCallSid(), session.getPhoneNo());

            long duration = (System.currentTimeMillis() - session.getStartTime()) / 1000;
            callLogService.logCall(
                    session.getPhoneNo(),
                    "employer",
                    session.getLanguagePreference(),
                    (int) duration,
                    "completed"
            );

            // ✅ Broadcast to dashboard
            webSocketLogService.logCallCompleted(session.getCallSid(), (int) duration);

            sessions.remove(session.getCallSid());

            String audioUrl = audioService.getAudioUrl("completion_employer", session.getLanguagePreference());
            Play play = new Play.Builder(audioUrl).build();

            VoiceResponse response = new VoiceResponse.Builder()
                    .play(play)
                    .hangup(new Hangup.Builder().build())
                    .build();

            return response.toXml();

        } catch (Exception e) {
            log.error("Error finalizing employer: {}", e.getMessage(), e);

            // ✅ Broadcast error to dashboard
            webSocketLogService.logError(session.getCallSid(), e.getMessage());

            return createErrorResponse("An error occurred. Please try again.");
        }
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
