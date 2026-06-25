package com.cloudbrain.application.workflow;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiContentAttachment;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiStreamSessionCreateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiStreamSessionCreateResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordGenerateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordSummary;
import com.cloudbrain.security.ActorContext;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiStreamSessionService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(5);
    private static final long EMITTER_TIMEOUT_MS = Duration.ofMinutes(2).toMillis();

    private final WorkflowService workflowService;
    private final Map<String, StreamSession> sessions = new ConcurrentHashMap<>();

    public AiStreamSessionService(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    public AiStreamSessionCreateResponse createSession(ActorContext actorContext, AiStreamSessionCreateRequest request) {
        if (actorContext == null || !actorContext.isDoctor()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "doctor permission required");
        }
        String taskType = normalizeTaskType(request.taskType());
        if (!"MEDICAL_RECORD".equals(taskType) && !"DIAGNOSIS".equals(taskType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "unsupported stream task type");
        }

        Instant expiresAt = Instant.now().plus(SESSION_TTL);
        String sessionId = UUID.randomUUID().toString();
        String streamToken = UUID.randomUUID().toString().replace("-", "");
        sessions.put(sessionId, new StreamSession(sessionId, streamToken, taskType, request, actorContext, expiresAt));
        return new AiStreamSessionCreateResponse(sessionId, streamToken, taskType, expiresAt);
    }

    public SseEmitter subscribe(String sessionId, String token) {
        StreamSession session = requireSession(sessionId, token);
        if (!session.markStarted()) {
            throw new ApiException(HttpStatus.CONFLICT.value(), "stream session already consumed");
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> sessions.remove(sessionId));
        emitter.onTimeout(() -> sessions.remove(sessionId));
        emitter.onError(ignored -> sessions.remove(sessionId));

        CompletableFuture.runAsync(() -> stream(session, emitter));
        return emitter;
    }

    public void cancel(ActorContext actorContext, String sessionId) {
        StreamSession session = sessions.get(sessionId);
        if (session == null) {
            return;
        }
        if (actorContext == null
                || !actorContext.isDoctor()
                || !actorContext.getRequiredDoctorId().equals(session.actorContext().getRequiredDoctorId())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "stream session permission required");
        }
        session.cancel();
        sessions.remove(sessionId);
    }

    private StreamSession requireSession(String sessionId, String token) {
        StreamSession session = sessions.get(sessionId);
        if (session == null || session.isExpired()) {
            sessions.remove(sessionId);
            throw new ApiException(HttpStatus.NOT_FOUND.value(), "stream session not found");
        }
        if (!session.streamToken().equals(token)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "invalid stream token");
        }
        return session;
    }

    private void stream(StreamSession session, SseEmitter emitter) {
        try {
            send(emitter, "meta", Map.of("sessionId", session.sessionId(), "taskType", session.taskType()));
            AtomicBoolean streamedChunks = new AtomicBoolean(false);
            java.util.function.Consumer<String> chunkConsumer = chunk -> {
                try {
                    streamedChunks.set(true);
                    send(emitter, "chunk", Map.of("text", chunk));
                } catch (IOException exception) {
                    throw new RuntimeException(exception);
                }
            };
            Object result = "MEDICAL_RECORD".equals(session.taskType())
                    ? workflowService.generateMedicalRecord(session.actorContext(), new MedicalRecordGenerateRequest(
                            session.request().registrationId(),
                            session.request().conversationText(),
                            session.request().diagnosisDirection(),
                            session.request().attachments()
                    ), chunkConsumer)
                    : workflowService.suggestDiagnosis(session.actorContext(), new DiagnosisSuggestionRequest(
                            session.request().registrationId(),
                            session.request().conversationText(),
                            session.request().diagnosisDirection(),
                            session.request().attachments()
                    ), chunkConsumer);
            String text = result instanceof MedicalRecordSummary medicalRecord
                    ? medicalRecordText(medicalRecord)
                    : diagnosisText((DiagnosisSuggestionResponse) result);
            if (!streamedChunks.get()) {
                streamText(session, emitter, text);
            }
            if (!session.isCancelled()) {
                send(emitter, "result", result);
                send(emitter, "done", Map.of("sessionId", session.sessionId()));
                emitter.complete();
            }
        } catch (Exception exception) {
            try {
                send(emitter, "error", Map.of("message", exception.getMessage() == null ? "stream failed" : exception.getMessage()));
            } catch (IOException ignored) {
                // best effort notification before the emitter is completed
            } finally {
                emitter.completeWithError(exception);
                sessions.remove(session.sessionId());
            }
        }
    }

    private void streamText(StreamSession session, SseEmitter emitter, String text) throws IOException, InterruptedException {
        List<String> chunks = splitChunks(text);
        for (String chunk : chunks) {
            if (session.isCancelled()) {
                send(emitter, "cancelled", Map.of("sessionId", session.sessionId()));
                emitter.complete();
                return;
            }
            send(emitter, "chunk", Map.of("text", chunk));
            Thread.sleep(80L);
        }
    }

    private List<String> splitChunks(String text) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return chunks;
        }
        int index = 0;
        int step = 18;
        while (index < text.length()) {
            int end = Math.min(text.length(), index + step);
            chunks.add(text.substring(index, end));
            index = end;
        }
        return chunks;
    }

    private void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(data));
    }

    private String medicalRecordText(MedicalRecordSummary record) {
        return String.join("\n",
                "chiefComplaint: " + safe(record.chiefComplaint()),
                "presentIllness: " + safe(record.presentIllness()),
                "pastHistory: " + safe(record.pastHistory()),
                "physicalExam: " + safe(record.physicalExam()),
                "preliminaryDiagnosis: " + safe(record.preliminaryDiagnosis()),
                "treatmentPlan: " + safe(record.treatmentPlan()),
                "docNote: " + safe(record.docNote())
        );
    }

    private String diagnosisText(DiagnosisSuggestionResponse response) {
        return String.join("\n",
                "suggestedDiagnoses: " + safe(response.suggestedDiagnoses()),
                "suggestedExamItems: " + safe(response.suggestedExamItems()),
                "summary: " + safe(response.summary())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizeTaskType(String taskType) {
        return taskType == null ? "" : taskType.trim().toUpperCase(Locale.ROOT).replace("-", "_");
    }

    private static final class StreamSession {
        private final String sessionId;
        private final String streamToken;
        private final String taskType;
        private final AiStreamSessionCreateRequest request;
        private final ActorContext actorContext;
        private final Instant expiresAt;
        private final AtomicBoolean started = new AtomicBoolean(false);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        private StreamSession(String sessionId,
                              String streamToken,
                              String taskType,
                              AiStreamSessionCreateRequest request,
                              ActorContext actorContext,
                              Instant expiresAt) {
            this.sessionId = sessionId;
            this.streamToken = streamToken;
            this.taskType = taskType;
            this.request = request;
            this.actorContext = actorContext;
            this.expiresAt = expiresAt;
        }

        String sessionId() {
            return sessionId;
        }

        String streamToken() {
            return streamToken;
        }

        String taskType() {
            return taskType;
        }

        AiStreamSessionCreateRequest request() {
            return request;
        }

        ActorContext actorContext() {
            return actorContext;
        }

        boolean markStarted() {
            return started.compareAndSet(false, true);
        }

        void cancel() {
            cancelled.set(true);
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        boolean isExpired() {
            return expiresAt.isBefore(Instant.now());
        }
    }
}
