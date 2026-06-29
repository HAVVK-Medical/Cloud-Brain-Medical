package com.cloudbrain.application.workflow;

import com.cloudbrain.application.ai.AIInvocationService;
import com.cloudbrain.application.ai.AIModels;
import com.cloudbrain.application.ai.AITextParser;
import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiUsageBucket;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiUsageStats;
import com.cloudbrain.dto.workflow.WorkflowDtos.ConversationTriageConfirmRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.ConsultationWorkspace;
import com.cloudbrain.dto.workflow.WorkflowDtos.DashboardOverview;
import com.cloudbrain.dto.workflow.WorkflowDtos.DashboardTrendPoint;
import com.cloudbrain.dto.workflow.WorkflowDtos.DepartmentOption;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionAdoptRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionIgnoreRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.DoctorOption;
import com.cloudbrain.dto.workflow.WorkflowDtos.DrugOption;
import com.cloudbrain.dto.workflow.WorkflowDtos.FeedbackCreateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.FeedbackResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiContentAttachment;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordGenerateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordSaveRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.NotificationRecordSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionItemRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionItemSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionRuleSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionReviewRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionReviewResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionSubmitRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionReviewRate;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationCancelRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationCreateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.RiskDistribution;
import com.cloudbrain.dto.workflow.WorkflowDtos.RiskDistributionBucket;
import com.cloudbrain.dto.workflow.WorkflowDtos.ScheduleOption;
import com.cloudbrain.dto.workflow.WorkflowDtos.TriageAccuracyStats;
import com.cloudbrain.dto.workflow.WorkflowDtos.TriageRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.TriageResponse;
import com.cloudbrain.entity.auth.DoctorEntity;
import com.cloudbrain.entity.auth.PatientEntity;
import com.cloudbrain.entity.core.AICallRecordEntity;
import com.cloudbrain.entity.core.AIConfigEntity;
import com.cloudbrain.entity.core.ConsultationNoteEntity;
import com.cloudbrain.entity.core.DepartmentEntity;
import com.cloudbrain.entity.core.DiagnosisSuggestionRecordEntity;
import com.cloudbrain.entity.core.DrugEntity;
import com.cloudbrain.entity.core.FeedbackEntity;
import com.cloudbrain.entity.core.MedicalRecordEntity;
import com.cloudbrain.entity.core.NotificationRecordEntity;
import com.cloudbrain.entity.core.PrescriptionEntity;
import com.cloudbrain.entity.core.PrescriptionItemEntity;
import com.cloudbrain.entity.core.PrescriptionReviewEntity;
import com.cloudbrain.entity.core.PrescriptionRuleDefinitionEntity;
import com.cloudbrain.entity.core.PromptTemplateEntity;
import com.cloudbrain.entity.core.RegistrationEntity;
import com.cloudbrain.entity.core.ScheduleEntity;
import com.cloudbrain.entity.core.TriageAccuracyFeedbackEntity;
import com.cloudbrain.entity.core.TriageRecordEntity;
import com.cloudbrain.repository.AICallRecordJpaRepository;
import com.cloudbrain.repository.AIConfigJpaRepository;
import com.cloudbrain.repository.ConsultationNoteJpaRepository;
import com.cloudbrain.repository.DepartmentJpaRepository;
import com.cloudbrain.repository.DiagnosisSuggestionRecordJpaRepository;
import com.cloudbrain.repository.DoctorJpaRepository;
import com.cloudbrain.repository.DrugJpaRepository;
import com.cloudbrain.repository.FeedbackJpaRepository;
import com.cloudbrain.repository.MedicalRecordJpaRepository;
import com.cloudbrain.repository.NotificationRecordJpaRepository;
import com.cloudbrain.repository.PatientJpaRepository;
import com.cloudbrain.repository.PrescriptionItemJpaRepository;
import com.cloudbrain.repository.PrescriptionJpaRepository;
import com.cloudbrain.repository.PrescriptionReviewJpaRepository;
import com.cloudbrain.repository.PrescriptionRuleDefinitionJpaRepository;
import com.cloudbrain.repository.PromptTemplateJpaRepository;
import com.cloudbrain.repository.RegistrationJpaRepository;
import com.cloudbrain.repository.ScheduleJpaRepository;
import com.cloudbrain.repository.TriageAccuracyFeedbackJpaRepository;
import com.cloudbrain.repository.TriageRecordJpaRepository;
import com.cloudbrain.security.ActorContext;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WorkflowService {

    private static final String ACTIVE = "ACTIVE";
    private static final String WAITING = "WAITING";
    private static final String IN_CONSULTATION = "IN_CONSULTATION";
    private static final String MEDICAL_RECORD_SAVED = "MEDICAL_RECORD_SAVED";
    private static final String PRESCRIPTION_REVIEWED = "PRESCRIPTION_REVIEWED";
    private static final String PRESCRIPTION_SUBMITTED = "PRESCRIPTION_SUBMITTED";
    private static final String COMPLETED = "COMPLETED";
    private static final String CANCELLED = "CANCELLED";

    private final DepartmentJpaRepository departmentRepository;
    private final DoctorJpaRepository doctorRepository;
    private final PatientJpaRepository patientRepository;
    private final ScheduleJpaRepository scheduleRepository;
    private final TriageRecordJpaRepository triageRecordRepository;
    private final RegistrationJpaRepository registrationRepository;
    private final ConsultationNoteJpaRepository consultationNoteRepository;
    private final MedicalRecordJpaRepository medicalRecordRepository;
    private final DiagnosisSuggestionRecordJpaRepository diagnosisSuggestionRepository;
    private final DrugJpaRepository drugRepository;
    private final PrescriptionJpaRepository prescriptionRepository;
    private final PrescriptionItemJpaRepository prescriptionItemRepository;
    private final PrescriptionReviewJpaRepository prescriptionReviewRepository;
    private final PrescriptionRuleDefinitionJpaRepository ruleRepository;
    private final AIConfigJpaRepository aiConfigRepository;
    private final PromptTemplateJpaRepository promptTemplateRepository;
    private final AIInvocationService aiInvocationService;
    private final NotificationRecordJpaRepository notificationRecordRepository;
    private final FeedbackJpaRepository feedbackRepository;
    private final TriageAccuracyFeedbackJpaRepository triageAccuracyFeedbackRepository;
    private final AICallRecordJpaRepository aiCallRecordRepository;
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final TransactionTemplate transactionTemplate;

    public WorkflowService(DepartmentJpaRepository departmentRepository,
                           DoctorJpaRepository doctorRepository,
                           PatientJpaRepository patientRepository,
                           ScheduleJpaRepository scheduleRepository,
                           TriageRecordJpaRepository triageRecordRepository,
                           RegistrationJpaRepository registrationRepository,
                           ConsultationNoteJpaRepository consultationNoteRepository,
                           MedicalRecordJpaRepository medicalRecordRepository,
                           DiagnosisSuggestionRecordJpaRepository diagnosisSuggestionRepository,
                           DrugJpaRepository drugRepository,
                           PrescriptionJpaRepository prescriptionRepository,
                           PrescriptionItemJpaRepository prescriptionItemRepository,
                           PrescriptionReviewJpaRepository prescriptionReviewRepository,
                           PrescriptionRuleDefinitionJpaRepository ruleRepository,
                           AIConfigJpaRepository aiConfigRepository,
                           PromptTemplateJpaRepository promptTemplateRepository,
                           AIInvocationService aiInvocationService,
                           NotificationRecordJpaRepository notificationRecordRepository,
                           FeedbackJpaRepository feedbackRepository,
                           TriageAccuracyFeedbackJpaRepository triageAccuracyFeedbackRepository,
                           AICallRecordJpaRepository aiCallRecordRepository,
                           NotificationWebSocketHandler notificationWebSocketHandler,
                           TransactionTemplate transactionTemplate) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.scheduleRepository = scheduleRepository;
        this.triageRecordRepository = triageRecordRepository;
        this.registrationRepository = registrationRepository;
        this.consultationNoteRepository = consultationNoteRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.diagnosisSuggestionRepository = diagnosisSuggestionRepository;
        this.drugRepository = drugRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.prescriptionItemRepository = prescriptionItemRepository;
        this.prescriptionReviewRepository = prescriptionReviewRepository;
        this.ruleRepository = ruleRepository;
        this.aiConfigRepository = aiConfigRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.aiInvocationService = aiInvocationService;
        this.notificationRecordRepository = notificationRecordRepository;
        this.feedbackRepository = feedbackRepository;
        this.triageAccuracyFeedbackRepository = triageAccuracyFeedbackRepository;
        this.aiCallRecordRepository = aiCallRecordRepository;
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.transactionTemplate = transactionTemplate;
    }

    @Transactional(readOnly = true)
    public List<DepartmentOption> listDepartments() {
        return departmentRepository.findByStatusOrderByNameAsc(ACTIVE).stream()
                .map(this::toDepartmentOption)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DoctorOption> listDoctors(Long departmentId) {
        Map<Long, DepartmentEntity> departments = departmentsById();
        List<DoctorEntity> doctors = departmentId == null
                ? doctorRepository.findAll().stream()
                        .filter(doctor -> ACTIVE.equals(doctor.getStatus()))
                        .sorted(Comparator.comparing(DoctorEntity::getName))
                        .toList()
                : doctorRepository.findByDepartmentIdAndStatusOrderByNameAsc(departmentId, ACTIVE);
        return doctors.stream()
                .map(doctor -> toDoctorOption(doctor, departments.get(doctor.getDepartmentId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorOption getDoctor(Long doctorId) {
        DoctorEntity doctor = doctorRepository.findByIdAndStatus(doctorId, ACTIVE)
                .orElseThrow(() -> notFound("doctor not found"));
        DepartmentEntity department = doctor.getDepartmentId() == null
                ? null
                : departmentRepository.findById(doctor.getDepartmentId()).orElse(null);
        return toDoctorOption(doctor, department);
    }

    @Transactional(readOnly = true)
    public DepartmentOption getDepartment(Long departmentId) {
        DepartmentEntity department = departmentRepository.findById(departmentId)
                .filter(entity -> ACTIVE.equals(entity.getStatus()))
                .orElseThrow(() -> notFound("department not found"));
        return toDepartmentOption(department);
    }

    @Transactional(readOnly = true)
    public List<ScheduleOption> listAvailableSchedules(Long departmentId) {
        Map<Long, DoctorEntity> doctors = doctorsById();
        Map<Long, DepartmentEntity> departments = departmentsById();
        List<ScheduleEntity> schedules = departmentId == null
                ? scheduleRepository.findByStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc(ACTIVE, 0)
                : scheduleRepository.findByDepartmentIdAndStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc(
                        departmentId,
                        ACTIVE,
                        0
                );
        return schedules.stream()
                .filter(schedule -> !schedule.getWorkDate().isBefore(LocalDate.now()))
                .map(schedule -> toScheduleOption(schedule, doctors.get(schedule.getDoctorId()), departments.get(schedule.getDepartmentId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleOption> listAllSchedules(Long departmentId) {
        Map<Long, DoctorEntity> doctors = doctorsById();
        Map<Long, DepartmentEntity> departments = departmentsById();
        return scheduleRepository.findAll().stream()
                .filter(schedule -> departmentId == null || Objects.equals(schedule.getDepartmentId(), departmentId))
                .filter(schedule -> ACTIVE.equals(schedule.getStatus()))
                .sorted(Comparator.comparing(ScheduleEntity::getWorkDate).thenComparing(ScheduleEntity::getPeriod))
                .map(schedule -> toScheduleOption(schedule, doctors.get(schedule.getDoctorId()), departments.get(schedule.getDepartmentId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleOption> listAvailableSchedulesForDoctor(Long doctorId) {
        DoctorEntity doctor = doctorRepository.findByIdAndStatus(doctorId, ACTIVE)
                .orElseThrow(() -> notFound("doctor not found"));
        Map<Long, DepartmentEntity> departments = departmentsById();
        return scheduleRepository.findAll().stream()
                .filter(schedule -> Objects.equals(schedule.getDoctorId(), doctorId))
                .filter(schedule -> ACTIVE.equals(schedule.getStatus()))
                .filter(schedule -> schedule.getRemainingSlots() != null && schedule.getRemainingSlots() > 0)
                .filter(schedule -> !schedule.getWorkDate().isBefore(LocalDate.now()))
                .sorted(Comparator.comparing(ScheduleEntity::getWorkDate).thenComparing(ScheduleEntity::getPeriod))
                .map(schedule -> toScheduleOption(schedule, doctor, departments.get(schedule.getDepartmentId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DrugOption> searchDrugs(String keyword) {
        String normalized = keyword == null || keyword.isBlank() ? "" : keyword.trim();
        List<DrugEntity> drugs;
        if (normalized.isBlank()) {
            drugs = drugRepository.findAll().stream()
                    .filter(drug -> ACTIVE.equals(drug.getStatus()))
                    .sorted(Comparator.comparing(DrugEntity::getName))
                    .toList();
        } else {
            Map<Long, DrugEntity> merged = new LinkedHashMap<>();
            drugRepository.findByNameContainingAndStatusOrderByNameAsc(normalized, ACTIVE)
                    .forEach(drug -> merged.put(drug.getId(), drug));
            drugRepository.findByPinyinCodeContainingAndStatusOrderByPinyinCodeAsc(normalized.toUpperCase(Locale.ROOT), ACTIVE)
                    .forEach(drug -> merged.put(drug.getId(), drug));
            drugs = List.copyOf(merged.values());
        }
        return drugs.stream().map(this::toDrugOption).toList();
    }

    @Transactional
    public TriageResponse triage(ActorContext actorContext, TriageRequest request) {
        Long patientId = requirePatient(actorContext);
        long started = System.currentTimeMillis();
        String chiefComplaint = request.chiefComplaint().trim();

        // Local rule: keyword-based department pick
        DepartmentEntity localDepartment = pickExpandedDepartment(chiefComplaint);
        Map<Long, DepartmentEntity> departments = departmentsById();
        List<DoctorOption> localDoctors = doctorRepository
                .findByDepartmentIdAndStatusOrderByNameAsc(localDepartment.getId(), ACTIVE)
                .stream()
                .map(doctor -> toDoctorOption(doctor, departments.get(doctor.getDepartmentId())))
                .toList();
        String localReason = buildTriageReason(chiefComplaint, localDepartment, localDoctors);

        // Call AI for triage recommendation
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "TRIAGE",
                localDepartment.getCode(),
                Map.of(
                        "chiefComplaint", chiefComplaint,
                        "departmentName", localDepartment.getName(),
                        "doctorNames", localDoctors.stream().map(DoctorOption::name).filter(Objects::nonNull).collect(Collectors.joining(", "))
                ),
                request.attachments(),
                localReason,
                false,
                null
        );

        // Parse AI response for department recommendation
        boolean degraded = aiOutcome.meta().degraded();
        Map<String, String> aiParsed = AITextParser.parseKeyValueBlock(aiOutcome.result());
        String aiDeptCode = AITextParser.firstNonBlank(aiParsed, "recommendedDepartmentCode", null);
        String aiDeptName = AITextParser.firstNonBlank(aiParsed, "recommendedDepartment", aiDeptCode);

        // Determine AI-recommended department
        DepartmentEntity aiDepartment = resolveActiveDepartment(aiDeptCode, aiDeptName).orElse(null);

        // Use AI recommendation if available and valid; otherwise fall back to local rule
        DepartmentEntity finalDepartment = (aiDepartment != null && ACTIVE.equals(aiDepartment.getStatus()))
                ? aiDepartment
                : localDepartment;
        boolean aiMatchedLocal = finalDepartment.getId().equals(localDepartment.getId());

        List<DoctorOption> finalDoctors;
        List<ScheduleOption> finalSchedules;
        if (aiMatchedLocal) {
            finalDoctors = localDoctors;
            finalSchedules = listAvailableSchedules(finalDepartment.getId());
        } else {
            finalDoctors = doctorRepository
                    .findByDepartmentIdAndStatusOrderByNameAsc(finalDepartment.getId(), ACTIVE)
                    .stream()
                    .map(doctor -> toDoctorOption(doctor, departments.get(doctor.getDepartmentId())))
                    .toList();
            finalSchedules = listAvailableSchedules(finalDepartment.getId());
        }

        String reason = firstNonBlank(aiOutcome.result(), localReason);

        AICallRecordEntity callRecord = aiCallRecord("TRIAGE", actorContext, chiefComplaint, reason, started, aiOutcome.meta());
        callRecord = aiCallRecordRepository.save(callRecord);

        TriageRecordEntity triageRecord = new TriageRecordEntity();
        triageRecord.setPatientId(patientId);
        triageRecord.setChiefComplaint(chiefComplaint);
        triageRecord.setRecommendedDept(finalDepartment.getName());
        triageRecord.setRecommendedDoctors(finalDoctors.stream().map(DoctorOption::name).collect(Collectors.joining(", ")));
        triageRecord.setAiResponseRaw(reason);
        triageRecord.setCallStatus("COMPLETED");
        triageRecord.setRecommendationSource(aiOutcome.meta().provider());
        triageRecord.setAiCallRecordId(callRecord.getId());
        triageRecord = triageRecordRepository.save(triageRecord);

        callRecord.setBusinessRecordId(triageRecord.getId());
        aiCallRecordRepository.save(callRecord);

        return new TriageResponse(
                triageRecord.getId(),
                chiefComplaint,
                finalDepartment.getId(),
                finalDepartment.getName(),
                finalDoctors,
                finalSchedules,
                reason,
                triageRecord.getCallStatus(),
                triageRecord.getRecommendationSource(),
                aiDepartment != null ? aiDepartment.getId() : null,
                aiDepartment != null ? aiDepartment.getName() : null,
                degraded
        );
    }

    @Transactional
    public TriageResponse confirmConversationTriage(ActorContext actorContext, ConversationTriageConfirmRequest request) {
        Long patientId = requirePatient(actorContext);
        long started = System.currentTimeMillis();
        String chiefComplaint = request.chiefComplaint().trim();

        DepartmentEntity localDepartment = pickExpandedDepartment(chiefComplaint);
        DepartmentEntity finalDepartment = resolveActiveDepartment(request.departmentCode(), request.department())
                .orElse(localDepartment);
        Map<Long, DepartmentEntity> departments = departmentsById();
        List<DoctorOption> finalDoctors = doctorRepository
                .findByDepartmentIdAndStatusOrderByNameAsc(finalDepartment.getId(), ACTIVE)
                .stream()
                .map(doctor -> toDoctorOption(doctor, departments.get(doctor.getDepartmentId())))
                .toList();
        List<ScheduleOption> finalSchedules = listAvailableSchedules(finalDepartment.getId());

        String reason = firstNonBlank(
                request.reason(),
                "根据 AI 对话式问诊结果，建议优先前往" + finalDepartment.getName() + "。"
        );
        if (request.urgencyLevel() != null && !request.urgencyLevel().isBlank()) {
            reason = reason + "\n紧急程度：" + request.urgencyLevel().trim();
        }

        AICallRecordEntity callRecord = aiCallRecord("TRIAGE", actorContext, chiefComplaint, reason, started);
        callRecord.setProvider("TRIAGE_CONVERSATION");
        callRecord.setModelName("conversation-triage");
        callRecord.setConfigVersion("conversation-v1");
        callRecord.setPromptVersion("conversation-v1");
        callRecord = aiCallRecordRepository.save(callRecord);

        TriageRecordEntity triageRecord = new TriageRecordEntity();
        triageRecord.setPatientId(patientId);
        triageRecord.setChiefComplaint(chiefComplaint);
        triageRecord.setRecommendedDept(finalDepartment.getName());
        triageRecord.setRecommendedDoctors(finalDoctors.stream().map(DoctorOption::name).collect(Collectors.joining(", ")));
        triageRecord.setAiResponseRaw(reason);
        triageRecord.setCallStatus("COMPLETED");
        triageRecord.setRecommendationSource("TRIAGE_CONVERSATION");
        triageRecord.setAiCallRecordId(callRecord.getId());
        triageRecord = triageRecordRepository.save(triageRecord);

        callRecord.setBusinessRecordId(triageRecord.getId());
        aiCallRecordRepository.save(callRecord);

        return new TriageResponse(
                triageRecord.getId(),
                chiefComplaint,
                finalDepartment.getId(),
                finalDepartment.getName(),
                finalDoctors,
                finalSchedules,
                reason,
                triageRecord.getCallStatus(),
                triageRecord.getRecommendationSource(),
                finalDepartment.getId(),
                finalDepartment.getName(),
                false
        );
    }

    @Transactional
    public RegistrationSummary createRegistration(ActorContext actorContext, RegistrationCreateRequest request) {
        Long patientId = requirePatient(actorContext);
        ScheduleEntity schedule = scheduleRepository.findById(request.scheduleId())
                .orElseThrow(() -> notFound("schedule not found"));
        if (!ACTIVE.equals(schedule.getStatus()) || schedule.getRemainingSlots() <= 0) {
            throw conflict("schedule is unavailable");
        }
        if (registrationRepository.existsByPatientIdAndScheduleIdAndStatusNot(patientId, schedule.getId(), CANCELLED)) {
            throw conflict("you already have an active registration for this schedule");
        }

        int updated = scheduleRepository.decrementSlotWithVersion(schedule.getId(), schedule.getVersion());
        if (updated != 1) {
            throw conflict("schedule slot changed, please retry");
        }
        schedule = scheduleRepository.findById(schedule.getId()).orElseThrow();

        DoctorEntity doctor = doctorRepository.findById(schedule.getDoctorId()).orElseThrow(() -> notFound("doctor not found"));
        DepartmentEntity department = departmentRepository.findById(schedule.getDepartmentId()).orElseThrow(() -> notFound("department not found"));

        RegistrationEntity registration = new RegistrationEntity();
        registration.setPatientId(patientId);
        registration.setDoctorId(doctor.getId());
        registration.setDepartmentId(department.getId());
        registration.setScheduleId(schedule.getId());
        registration.setTriageRecordId(request.triageRecordId());
        registration.setRegistrationTime(LocalDateTime.now());
        registration.setStatus(WAITING);
        registration.setDepartmentSnapshot(department.getName());
        registration.setDoctorSnapshot(doctor.getName());
        registration.setVisitLevelSnapshot(schedule.getVisitLevel());
        registration.setSlotReleased(false);
        registration.setVersion(0);
        final Long scheduleId = schedule.getId();
        try {
            registration = registrationRepository.saveAndFlush(registration);
        } catch (DataIntegrityViolationException exception) {
            // Release the slot in a new transaction because the current one is marked rollback-only
            transactionTemplate.executeWithoutResult(status ->
                    scheduleRepository.releaseSlotOnce(scheduleId));
            throw conflict("you already have an active registration for this schedule");
        }

        if (request.triageRecordId() != null) {
            Long savedRegistrationId = registration.getId();
            triageRecordRepository.findById(request.triageRecordId()).ifPresent(record -> {
                if (Objects.equals(record.getPatientId(), patientId)) {
                    record.setRegistrationId(savedRegistrationId);
                    triageRecordRepository.save(record);
                }
            });
        }
        return toRegistrationSummary(registration);
    }

    @Transactional
    public RegistrationSummary cancelRegistration(ActorContext actorContext, Long registrationId, RegistrationCancelRequest request) {
        Long patientId = requirePatient(actorContext);
        RegistrationEntity registration = registrationRepository.findByIdAndPatientId(registrationId, patientId)
                .orElseThrow(() -> notFound("registration not found"));
        if (!WAITING.equals(registration.getStatus())) {
            throw conflict("only waiting registrations can be cancelled");
        }
        int cancelled = registrationRepository.cancelWaitingRegistrationOnce(
                registration.getId(),
                patientId,
                WAITING,
                CANCELLED,
                request == null || request.reason() == null || request.reason().isBlank() ? "patient cancelled" : request.reason(),
                LocalDateTime.now()
        );
        if (cancelled == 1) {
            scheduleRepository.releaseSlotOnce(registration.getScheduleId());
        }
        return toRegistrationSummary(registrationRepository.findById(registrationId).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<RegistrationSummary> listPatientRegistrations(ActorContext actorContext) {
        Long patientId = requirePatient(actorContext);
        return registrationRepository.findByPatientIdOrderByRegistrationTimeDesc(patientId).stream()
                .map(this::toRegistrationSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrationSummary> listDoctorQueue(ActorContext actorContext) {
        Long doctorId = requireDoctor(actorContext);
        return registrationRepository.findByDoctorIdAndStatusInOrderByRegistrationTimeAsc(
                        doctorId,
                        List.of(WAITING, IN_CONSULTATION, MEDICAL_RECORD_SAVED, PRESCRIPTION_REVIEWED, PRESCRIPTION_SUBMITTED)
                ).stream()
                .map(this::toRegistrationSummary)
                .toList();
    }

    @Transactional
    public RegistrationSummary startConsultation(ActorContext actorContext, Long registrationId) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = registrationRepository.findByIdAndDoctorId(registrationId, doctorId)
                .orElseThrow(() -> notFound("registration not found"));
        if (WAITING.equals(registration.getStatus())) {
            registration.setStatus(IN_CONSULTATION);
            registration.setConsultationStartTime(LocalDateTime.now());
            registration = registrationRepository.save(registration);
        } else if (!List.of(IN_CONSULTATION, MEDICAL_RECORD_SAVED, PRESCRIPTION_REVIEWED, PRESCRIPTION_SUBMITTED, COMPLETED)
                .contains(registration.getStatus())) {
            throw conflict("registration cannot start consultation from status " + registration.getStatus());
        }
        return toRegistrationSummary(registration);
    }

    @Transactional(readOnly = true)
    public ConsultationWorkspace getConsultationWorkspace(ActorContext actorContext, Long registrationId) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(registrationId, doctorId);
        MedicalRecordSummary latestRecord = medicalRecordRepository.findFirstByRegistrationIdOrderByVersionDesc(registrationId)
                .map(this::toMedicalRecordSummary)
                .orElse(null);
        PrescriptionSummary latestPrescription = prescriptionRepository.findByRegistrationIdOrderByCreatedAtDesc(registrationId).stream()
                .findFirst()
                .map(this::toPrescriptionSummary)
                .orElse(null);
        List<PrescriptionReviewResponse> recentReviews = prescriptionReviewRepository
                .findByRegistrationIdOrderByCreatedAtDesc(registrationId)
                .stream()
                .map(review -> toReviewResponse(
                        review,
                        review.getPrescriptionId(),
                        prescriptionItemsFor(review.getPrescriptionId())))
                .toList();
        return new ConsultationWorkspace(
                toRegistrationSummary(registration),
                latestRecord,
                latestPrescription,
                recentReviews,
                nextActions(registration)
        );
    }

    @Transactional
    public RegistrationSummary completeConsultation(ActorContext actorContext, Long registrationId) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(registrationId, doctorId);
        if (COMPLETED.equals(registration.getStatus())) {
            return toRegistrationSummary(registration);
        }
        if (!List.of(MEDICAL_RECORD_SAVED, PRESCRIPTION_REVIEWED, PRESCRIPTION_SUBMITTED).contains(registration.getStatus())) {
            throw conflict("consultation can only be completed after medical record is saved");
        }
        registration.setStatus(COMPLETED);
        if (registration.getCompletedTime() == null) {
            registration.setCompletedTime(LocalDateTime.now());
        }
        return toRegistrationSummary(registrationRepository.save(registration));
    }

    @Transactional
    public MedicalRecordSummary generateMedicalRecord(ActorContext actorContext, MedicalRecordGenerateRequest request) {
        return generateMedicalRecord(actorContext, request, null);
    }

    @Transactional
    public MedicalRecordSummary generateMedicalRecord(ActorContext actorContext,
                                                      MedicalRecordGenerateRequest request,
                                                      Consumer<String> chunkConsumer) {
        return generateMedicalRecord(actorContext, request, chunkConsumer, null);
    }

    @Transactional
    public MedicalRecordSummary generateMedicalRecord(ActorContext actorContext,
                                                      MedicalRecordGenerateRequest request,
                                                      Consumer<String> chunkConsumer,
                                                      Consumer<String> thinkingConsumer) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(request.registrationId(), doctorId);
        PatientEntity patient = patientRepository.findById(registration.getPatientId()).orElseThrow(() -> notFound("patient not found"));
        long started = System.currentTimeMillis();
        String chief = firstSentence(request.conversationText());
        String fallbackDiagnosis = firstNonBlank(request.diagnosisDirection(), inferDiagnosis(request.conversationText()));
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("conversationText", request.conversationText());
        variables.put("diagnosisDirection", fallbackDiagnosis);
        String departmentName = departmentNameByRegistration(registration);
        if (departmentName != null) {
            variables.put("departmentName", departmentName);
        }
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "MEDICAL_RECORD",
                departmentCodeByRegistration(registration),
                variables,
                request.attachments(),
                buildMedicalRecordFallbackText(chief, request.conversationText(), patient, fallbackDiagnosis),
                chunkConsumer != null,
                chunkConsumer,
                thinkingConsumer
        );
        MedicalRecordDraft aiDraft = parseMedicalRecordDraft(aiOutcome.result());
        String diagnosis = firstNonBlank(aiDraft.preliminaryDiagnosis(), fallbackDiagnosis);

        MedicalRecordEntity draft = new MedicalRecordEntity();
        draft.setPatientId(patient.getId());
        draft.setDoctorId(doctorId);
        draft.setRegistrationId(registration.getId());
        draft.setChiefComplaint(firstNonBlank(aiDraft.chiefComplaint(), chief));
        draft.setPresentIllness(firstNonBlank(aiDraft.presentIllness(), request.conversationText()));
        draft.setPastHistory(firstNonBlank(aiDraft.pastHistory(), firstNonBlank(patient.getMedicalHistory(), "未见特殊既往史")));
        draft.setPhysicalExam(firstNonBlank(aiDraft.physicalExam(), "生命体征平稳，建议医生结合查体补充。"));
        draft.setPreliminaryDiagnosis(diagnosis);
        draft.setTreatmentPlan(firstNonBlank(aiDraft.treatmentPlan(), buildTreatmentPlan(diagnosis)));
        draft.setConversationText(request.conversationText());
        draft.setAiGenerated(true);
        draft.setDocNote(firstNonBlank(aiDraft.docNote(), "本地模拟 AI 已根据问诊文本生成结构化病历草稿，请医生确认。"));
        draft.setVersion(0);

        String output = draft.getChiefComplaint() + " | " + draft.getPreliminaryDiagnosis();
        AICallRecordEntity callRecord = aiCallRecord("MEDICAL_RECORD", actorContext, request.conversationText(), output, started, aiOutcome.meta());
        callRecord = aiCallRecordRepository.save(callRecord);
        draft.setAiCallRecordId(callRecord.getId());
        callRecord.setBusinessRecordId(registration.getId());
        aiCallRecordRepository.save(callRecord);

        // Persist the draft so it survives page refreshes / navigation
        draft = medicalRecordRepository.save(draft);
        return toMedicalRecordSummary(draft, aiOutcome.meta().degraded());
    }

    @Transactional
    public MedicalRecordSummary saveMedicalRecord(ActorContext actorContext, MedicalRecordSaveRequest request) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(request.registrationId(), doctorId);
        if (!List.of(IN_CONSULTATION, MEDICAL_RECORD_SAVED, PRESCRIPTION_REVIEWED).contains(registration.getStatus())) {
            String currentStatus = registration.getStatus() == null ? "未知" : registration.getStatus();
            String hint = switch (currentStatus) {
                case "WAITING" -> "请先点击“开始接诊”后再保存病历";
                case "COMPLETED" -> "该就诊已完成，无法再保存病历";
                case "CANCELLED" -> "该挂号已取消，无法保存病历";
                default -> "当前就诊状态(" + currentStatus + ")不允许保存病历";
            };
            throw conflict(hint);
        }

        ConsultationNoteEntity note = new ConsultationNoteEntity();
        note.setRegistrationId(registration.getId());
        note.setDoctorId(doctorId);
        note.setConversationText(request.conversationText());
        note.setChiefComplaintSummary(request.chiefComplaint());
        note.setDiagnosisDirection(request.preliminaryDiagnosis());
        note.setPatientContext("patientId=" + registration.getPatientId());
        consultationNoteRepository.save(note);

        MedicalRecordEntity record = medicalRecordRepository
                .findFirstByRegistrationIdOrderByVersionDesc(registration.getId())
                .orElseGet(MedicalRecordEntity::new);
        record.setPatientId(registration.getPatientId());
        record.setDoctorId(doctorId);
        record.setRegistrationId(registration.getId());
        record.setChiefComplaint(firstNonBlank(request.chiefComplaint(), "未填写"));
        record.setPresentIllness(firstNonBlank(request.presentIllness(), request.conversationText()));
        record.setPastHistory(firstNonBlank(request.pastHistory(), "未填写"));
        record.setPhysicalExam(firstNonBlank(request.physicalExam(), "未填写"));
        record.setPreliminaryDiagnosis(firstNonBlank(request.preliminaryDiagnosis(), "待明确"));
        record.setTreatmentPlan(firstNonBlank(request.treatmentPlan(), "遵医嘱治疗，必要时复诊。"));
        record.setConversationText(request.conversationText());
        record.setAiGenerated(Boolean.TRUE.equals(request.aiGenerated()));
        record.setDocNote(request.docNote());
        if (record.getVersion() == null) {
            record.setVersion(0);
        }
        record = medicalRecordRepository.save(record);

        registration.setStatus(MEDICAL_RECORD_SAVED);
        registration.setRecordConfirmedTime(LocalDateTime.now());
        registrationRepository.save(registration);
        return toMedicalRecordSummary(record);
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordSummary> listPatientMedicalRecords(ActorContext actorContext) {
        Long patientId = requirePatient(actorContext);
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toMedicalRecordSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordSummary> listMedicalRecordsForPatient(ActorContext actorContext, Long patientId) {
        if (actorContext == null
                || !(actorContext.isAdmin()
                || (actorContext.isPatient() && Objects.equals(actorContext.patientId(), patientId))
                || actorContext.isDoctor())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "medical record permission required");
        }
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .filter(record -> actorContext.isPatient()
                        || actorContext.isAdmin()
                        || Objects.equals(record.getDoctorId(), actorContext.doctorId()))
                .map(this::toMedicalRecordSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordSummary> listDoctorMedicalRecords(ActorContext actorContext) {
        Long doctorId = requireDoctor(actorContext);
        return medicalRecordRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId).stream()
                .map(this::toMedicalRecordSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MedicalRecordSummary> searchDoctorMedicalRecords(ActorContext actorContext, String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return listDoctorMedicalRecords(actorContext).stream()
                .filter(record -> normalized.isBlank()
                        || containsNormalized(record.patientName(), normalized)
                        || String.valueOf(record.patientId()).contains(normalized)
                        || containsNormalized(record.chiefComplaint(), normalized)
                        || containsNormalized(record.preliminaryDiagnosis(), normalized))
                .toList();
    }

    @Transactional(readOnly = true)
    public MedicalRecordSummary getMedicalRecord(ActorContext actorContext, Long recordId) {
        MedicalRecordEntity record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> notFound("medical record not found"));
        if (actorContext == null
                || !(actorContext.isAdmin()
                || (actorContext.isPatient() && Objects.equals(record.getPatientId(), actorContext.patientId()))
                || (actorContext.isDoctor() && Objects.equals(record.getDoctorId(), actorContext.doctorId())))) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "medical record permission required");
        }
        return toMedicalRecordSummary(record);
    }

    @Transactional
    public DiagnosisSuggestionResponse suggestDiagnosis(ActorContext actorContext, DiagnosisSuggestionRequest request) {
        return suggestDiagnosis(actorContext, request, null);
    }

    @Transactional
    public DiagnosisSuggestionResponse suggestDiagnosis(ActorContext actorContext,
                                                       DiagnosisSuggestionRequest request,
                                                       Consumer<String> chunkConsumer) {
        return suggestDiagnosis(actorContext, request, chunkConsumer, null);
    }

    @Transactional
    public DiagnosisSuggestionResponse suggestDiagnosis(ActorContext actorContext,
                                                       DiagnosisSuggestionRequest request,
                                                       Consumer<String> chunkConsumer,
                                                       Consumer<String> thinkingConsumer) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(request.registrationId(), doctorId);
        long started = System.currentTimeMillis();
        String fallbackDiagnosis = inferDiagnosis(request.conversationText());
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("conversationText", request.conversationText());
        variables.put("diagnosisDirection", firstNonBlank(request.diagnosisDirection(), fallbackDiagnosis));
        String departmentName = departmentNameByRegistration(registration);
        if (departmentName != null) {
            variables.put("departmentName", departmentName);
        }
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "DIAGNOSIS",
                departmentCodeByRegistration(registration),
                variables,
                request.attachments(),
                String.join("\n",
                        "suggestedDiagnoses: " + fallbackDiagnosis + "；鉴别：焦虑相关不适、消化系统不适、呼吸系统感染。",
                        "suggestedExamItems: " + suggestExamItems(fallbackDiagnosis),
                        "summary: 本地规则根据关键词生成诊疗建议，供医生参考。",
                        "finalDiagnosisDirection: " + fallbackDiagnosis
                ),
                chunkConsumer != null,
                chunkConsumer,
                thinkingConsumer
        );
        Map<String, String> parsed = AITextParser.parseKeyValueBlock(aiOutcome.result());
        String diagnosis = firstNonBlank(AITextParser.firstNonBlank(parsed, "finalDiagnosisDirection", null), fallbackDiagnosis);
        DiagnosisSuggestionRecordEntity entity = new DiagnosisSuggestionRecordEntity();
        entity.setRegistrationId(registration.getId());
        entity.setPatientId(registration.getPatientId());
        entity.setDoctorId(doctorId);
        entity.setSuggestedDiagnoses(firstNonBlank(AITextParser.firstNonBlank(parsed, "suggestedDiagnoses", null), diagnosis + "\n鉴别：焦虑相关不适、消化系统不适、呼吸系统感染。"));
        entity.setSuggestedExamItems(firstNonBlank(AITextParser.firstNonBlank(parsed, "suggestedExamItems", null), suggestExamItems(diagnosis)));
        entity.setAdoptionStatus("SUGGESTED");
        entity.setFinalDiagnosisDirection(diagnosis);
        entity = diagnosisSuggestionRepository.save(entity);

        AICallRecordEntity callRecord = aiCallRecord(
                "DIAGNOSIS",
                actorContext,
                request.conversationText(),
                entity.getSuggestedDiagnoses(),
                started,
                aiOutcome.meta()
        );
        callRecord.setBusinessRecordId(entity.getId());
        callRecord = aiCallRecordRepository.save(callRecord);
        entity.setAiCallRecordId(callRecord.getId());
        entity = diagnosisSuggestionRepository.save(entity);
        return new DiagnosisSuggestionResponse(
                entity.getId(),
                entity.getRegistrationId(),
                entity.getSuggestedDiagnoses(),
                entity.getSuggestedExamItems(),
                entity.getAdoptionStatus(),
                "本地规则根据关键词生成诊疗建议，供医生参考。",
                aiOutcome.meta().degraded()
        );
    }

    @Transactional
    public DiagnosisSuggestionResponse adoptDiagnosisSuggestion(ActorContext actorContext,
                                                               Long suggestionId,
                                                               DiagnosisSuggestionAdoptRequest request) {
        Long doctorId = requireDoctor(actorContext);
        DiagnosisSuggestionRecordEntity entity = requireDoctorDiagnosisSuggestion(suggestionId, doctorId);
        String finalDiagnosis = firstNonBlank(request.finalDiagnosis(), entity.getFinalDiagnosisDirection());
        entity.setAdoptionStatus("ADOPTED");
        entity.setFinalDiagnosisDirection(finalDiagnosis);
        entity.setAdoptionDoctorId(doctorId);
        entity.setAdoptionTime(LocalDateTime.now());
        entity = diagnosisSuggestionRepository.save(entity);
        return toDiagnosisSuggestionResponse(entity, "已采纳诊断建议。", false);
    }

    @Transactional
    public DiagnosisSuggestionResponse ignoreDiagnosisSuggestion(ActorContext actorContext,
                                                                Long suggestionId,
                                                                DiagnosisSuggestionIgnoreRequest request) {
        Long doctorId = requireDoctor(actorContext);
        DiagnosisSuggestionRecordEntity entity = requireDoctorDiagnosisSuggestion(suggestionId, doctorId);
        entity.setAdoptionStatus("IGNORED");
        entity.setAdoptionDoctorId(doctorId);
        entity.setAdoptionTime(LocalDateTime.now());
        String reason = request == null ? null : blankToNull(request.reason());
        if (reason != null) {
            entity.setFinalDiagnosisDirection(reason);
        }
        entity = diagnosisSuggestionRepository.save(entity);
        return toDiagnosisSuggestionResponse(entity, "已忽略诊断建议。", false);
    }

    @Transactional
    public PrescriptionReviewResponse reviewPrescription(ActorContext actorContext, PrescriptionReviewRequest request) {
        return reviewPrescription(actorContext, request, null);
    }

    @Transactional
    public PrescriptionReviewResponse reviewPrescription(ActorContext actorContext,
                                                        PrescriptionReviewRequest request,
                                                        Consumer<String> chunkConsumer) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(request.registrationId(), doctorId);
        if (!List.of(MEDICAL_RECORD_SAVED, PRESCRIPTION_REVIEWED, PRESCRIPTION_SUBMITTED).contains(registration.getStatus())) {
            throw conflict("please save medical record before prescription review");
        }
        List<PrescriptionItemRequest> items = requirePrescriptionItems(request.items());
        long started = System.currentTimeMillis();

        List<DrugEntity> drugs = items.stream()
                .map(item -> drugRepository.findByIdAndStatus(item.drugId(), ACTIVE)
                        .orElseThrow(() -> notFound("drug not found: " + item.drugId())))
                .toList();
        ReviewComputation review = computeReview(registration, items, drugs);
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("riskLevel", review.riskLevel());
        variables.put("localRuleHits", review.localRuleHits());
        variables.put("missingItems", review.contextMissingItems());
        variables.put("prescriptionSummary", review.summary());
        AIModels.AIExecutionOutcome<String> aiOutcome = invokeAi(
                "PRESCRIPTION_REVIEW",
                departmentCodeByRegistration(registration),
                variables,
                request.attachments(),
                String.join("\n",
                        "llmSuggestion: " + review.suggestion(),
                        "llmSummary: " + review.summary()
                ),
                chunkConsumer != null,
                chunkConsumer
        );
        Map<String, String> reviewParsed = AITextParser.parseKeyValueBlock(aiOutcome.result());
        AICallRecordEntity callRecord = aiCallRecord(
                "PRESCRIPTION_REVIEW",
                actorContext,
                drugs.stream().map(DrugEntity::getName).collect(Collectors.joining(", ")),
                firstNonBlank(AITextParser.firstNonBlank(reviewParsed, "llmSummary", null), review.summary()),
                started,
                aiOutcome.meta()
        );
        callRecord = aiCallRecordRepository.save(callRecord);

        PrescriptionReviewEntity entity = new PrescriptionReviewEntity();
        entity.setRegistrationId(registration.getId());
        entity.setDoctorId(doctorId);
        entity.setPatientId(registration.getPatientId());
        entity.setRiskLevel(review.riskLevel());
        entity.setLocalRuleHits(review.localRuleHits());
        entity.setRuleEngineStatus("COMPLETED");
        entity.setContextMissingItems(review.contextMissingItems());
        entity.setLlmSuggestion(firstNonBlank(AITextParser.firstNonBlank(reviewParsed, "llmSuggestion", null), review.suggestion()));
        entity.setLlmSummary(firstNonBlank(AITextParser.firstNonBlank(reviewParsed, "llmSummary", null), review.summary()));
        entity.setLlmCallStatus(aiOutcome.meta().provider());
        entity.setAiCallRecordId(callRecord.getId());
        entity.setPrescriptionSnapshotHash(hashItems(items));
        entity.setReviewContextHash(hashContext(registration));
        entity.setBindStatus("UNBOUND");
        entity.setVersion(0);
        entity = prescriptionReviewRepository.save(entity);
        callRecord.setBusinessRecordId(entity.getId());
        aiCallRecordRepository.save(callRecord);

        upsertPrescriptionReviewNotification(registration, entity);

        registration.setStatus(PRESCRIPTION_REVIEWED);
        registrationRepository.save(registration);
        return toReviewResponse(entity, null, toItemSummariesFromRequest(items, drugs));
    }

    @Transactional
    public PrescriptionSummary submitPrescription(ActorContext actorContext, PrescriptionSubmitRequest request) {
        Long doctorId = requireDoctor(actorContext);
        RegistrationEntity registration = requireDoctorRegistration(request.registrationId(), doctorId);
        List<PrescriptionItemRequest> items = requirePrescriptionItems(request.items());
        PrescriptionReviewEntity review = prescriptionReviewRepository.findByIdAndBindStatus(request.reviewId(), "UNBOUND")
                .orElseThrow(() -> notFound("unbound review not found"));
        if (!Objects.equals(review.getRegistrationId(), registration.getId())) {
            throw conflict("review does not belong to this registration");
        }
        String itemHash = hashItems(items);
        String contextHash = hashContext(registration);
        if (!Objects.equals(review.getPrescriptionSnapshotHash(), itemHash)
                || !Objects.equals(review.getReviewContextHash(), contextHash)) {
            throw conflict("prescription changed after review, please review again");
        }

        List<DrugEntity> drugs = items.stream()
                .map(item -> drugRepository.findByIdAndStatus(item.drugId(), ACTIVE)
                        .orElseThrow(() -> notFound("drug not found: " + item.drugId())))
                .toList();

        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setPatientId(registration.getPatientId());
        prescription.setDoctorId(doctorId);
        prescription.setRegistrationId(registration.getId());
        prescription.setReviewId(review.getId());
        prescription.setRiskLevel(review.getRiskLevel());
        prescription.setStatus("SUBMITTED");
        prescription = prescriptionRepository.save(prescription);

        for (int i = 0; i < items.size(); i++) {
            prescriptionItemRepository.save(toPrescriptionItem(prescription.getId(), items.get(i), drugs.get(i)));
        }

        review.setPrescriptionId(prescription.getId());
        review.setBindStatus("BOUND");
        review.setManualConfirmation(request.manualConfirmation());
        prescriptionReviewRepository.save(review);

        registration.setStatus(COMPLETED);
        registration.setPrescriptionSubmittedTime(LocalDateTime.now());
        registration.setCompletedTime(LocalDateTime.now());
        registrationRepository.save(registration);
        return toPrescriptionSummary(prescription);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionSummary> listPatientPrescriptions(ActorContext actorContext) {
        Long patientId = requirePatient(actorContext);
        return prescriptionRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toPrescriptionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrescriptionSummary> listDoctorPrescriptions(ActorContext actorContext) {
        Long doctorId = requireDoctor(actorContext);
        return prescriptionRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId).stream()
                .map(this::toPrescriptionSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrescriptionSummary getPrescription(ActorContext actorContext, Long prescriptionId) {
        PrescriptionEntity prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> notFound("prescription not found"));
        if (actorContext == null
                || !(actorContext.isAdmin()
                || (actorContext.isPatient() && Objects.equals(prescription.getPatientId(), actorContext.patientId()))
                || (actorContext.isDoctor() && Objects.equals(prescription.getDoctorId(), actorContext.doctorId())))) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "prescription permission required");
        }
        return toPrescriptionSummary(prescription);
    }

    @Transactional(readOnly = true)
    public List<NotificationRecordSummary> listUnreadNotifications(ActorContext actorContext) {
        if (actorContext == null || !(actorContext.isDoctor() || actorContext.isAdmin())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "notification permission required");
        }
        List<NotificationRecordEntity> notifications = actorContext.isAdmin()
                ? notificationRecordRepository.findByReadFalseOrderByCreatedAtDesc()
                : notificationRecordRepository.findByRecipientIdAndRecipientRoleAndReadFalseOrderByCreatedAtDesc(
                        actorContext.getRequiredDoctorId(),
                        "DOCTOR"
                );
        return notifications.stream()
                .map(this::toNotificationRecordSummary)
                .toList();
    }

    @Transactional
    public NotificationRecordSummary markNotificationRead(ActorContext actorContext, Long notificationId) {
        NotificationRecordEntity notification = notificationRecordRepository.findById(notificationId)
                .orElseThrow(() -> notFound("notification not found"));
        if (!canViewNotification(actorContext, notification)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "notification permission required");
        }
        if (!Boolean.TRUE.equals(notification.getRead())) {
            notification.setRead(true);
            notification = notificationRecordRepository.save(notification);
        }
        return toNotificationRecordSummary(notification);
    }

    @Transactional
    public FeedbackResponse createFeedback(ActorContext actorContext, FeedbackCreateRequest request) {
        Long patientId = requirePatient(actorContext);
        RegistrationEntity registration = registrationRepository.findByIdAndPatientId(request.registrationId(), patientId)
                .orElseThrow(() -> notFound("registration not found"));
        if (!COMPLETED.equals(registration.getStatus())) {
            throw conflict("feedback can only be submitted after completion");
        }
        Optional<FeedbackEntity> existing = feedbackRepository.findByRegistrationId(registration.getId());
        if (existing.isPresent()) {
            ensureTriageAccuracyFeedback(existing.get(), registration);
            return toFeedbackResponse(existing.get());
        }
        FeedbackEntity feedback = new FeedbackEntity();
        feedback.setPatientId(patientId);
        feedback.setRegistrationId(registration.getId());
        feedback.setRating(request.rating());
        feedback.setTriageAccurate(request.triageAccurate());
        feedback.setComment(request.comment());
        feedback = feedbackRepository.save(feedback);
        ensureTriageAccuracyFeedback(feedback, registration);
        return toFeedbackResponse(feedback);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> listPatientFeedback(ActorContext actorContext) {
        Long patientId = requirePatient(actorContext);
        return feedbackRepository.findByPatientIdOrderByCreatedAtDesc(patientId).stream()
                .map(this::toFeedbackResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardOverview overview(ActorContext actorContext) {
        if (actorContext == null || !(actorContext.isDoctor() || actorContext.isAdmin())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "dashboard permission required");
        }
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        List<RegistrationEntity> registrations = visibleRegistrations(actorContext);
        long todayRegistrations = registrations.stream()
                .filter(registration -> !registration.getRegistrationTime().isBefore(dayStart)
                        && registration.getRegistrationTime().isBefore(dayEnd))
                .count();
        long todayVisits = registrations.stream()
                .filter(registration -> !registration.getRegistrationTime().isBefore(dayStart)
                        && registration.getRegistrationTime().isBefore(dayEnd))
                .filter(registration -> List.of(IN_CONSULTATION, MEDICAL_RECORD_SAVED, PRESCRIPTION_REVIEWED, PRESCRIPTION_SUBMITTED, COMPLETED)
                        .contains(registration.getStatus()))
                .count();
        long waiting = registrations.stream().filter(registration -> WAITING.equals(registration.getStatus())).count();
        long completed = registrations.stream().filter(registration -> COMPLETED.equals(registration.getStatus())).count();
        Set<Long> visibleRegistrationIds = registrations.stream().map(RegistrationEntity::getId).collect(Collectors.toSet());
        long todayPrescriptions = prescriptionRepository.findAll().stream()
                .filter(prescription -> visibleRegistrationIds.contains(prescription.getRegistrationId()))
                .filter(prescription -> isSameDay(prescription.getCreatedAt(), dayStart, dayEnd))
                .count();
        long todayAiCalls = aiCallRecordRepository.findAll().stream()
                .filter(record -> isSameDay(record.getCreatedAt(), dayStart, dayEnd))
                .count();
        long medicalRecords = medicalRecordRepository.findAll().stream()
                .filter(record -> visibleRegistrationIds.contains(record.getRegistrationId()))
                .count();
        long prescriptions = prescriptionRepository.findAll().stream()
                .filter(prescription -> visibleRegistrationIds.contains(prescription.getRegistrationId()))
                .count();
        long highRisk = prescriptionReviewRepository.findAll().stream()
                .filter(review -> visibleRegistrationIds.contains(review.getRegistrationId()))
                .filter(review -> "HIGH".equals(review.getRiskLevel()) || "MEDIUM".equals(review.getRiskLevel()))
                .count();
        return new DashboardOverview(
                todayRegistrations,
                todayVisits,
                waiting,
                completed,
                todayPrescriptions,
                todayAiCalls,
                medicalRecords,
                prescriptions,
                highRisk,
                feedbackRepository.count(),
                aiCallRecordRepository.count(),
                Instant.now()
        );
    }

    @Transactional(readOnly = true)
    public List<DashboardTrendPoint> dashboardTrends(ActorContext actorContext, LocalDate startDate, LocalDate endDate) {
        requireDashboardActor(actorContext);
        LocalDate end = endDate == null ? LocalDate.now() : endDate;
        LocalDate start = startDate == null ? end.minusDays(13) : startDate;
        if (start.isAfter(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "startDate must not be after endDate");
        }
        if (start.plusDays(60).isBefore(end)) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "date range cannot exceed 60 days");
        }

        List<RegistrationEntity> registrations = visibleRegistrations(actorContext);
        Set<Long> registrationIds = registrations.stream().map(RegistrationEntity::getId).collect(Collectors.toSet());
        List<PrescriptionEntity> prescriptions = prescriptionRepository.findAll().stream()
                .filter(prescription -> registrationIds.contains(prescription.getRegistrationId()))
                .toList();
        List<AICallRecordEntity> aiRecords = visibleAiCallRecords(actorContext, registrations);

        List<DashboardTrendPoint> points = new ArrayList<>();
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            LocalDate current = date;
            LocalDateTime dayStart = current.atStartOfDay();
            LocalDateTime dayEnd = dayStart.plusDays(1);
            long registrationCount = registrations.stream()
                    .filter(registration -> isBetween(registration.getRegistrationTime(), dayStart, dayEnd))
                    .count();
            long visitCount = registrations.stream()
                    .filter(registration -> isBetween(registration.getRegistrationTime(), dayStart, dayEnd))
                    .filter(registration -> List.of(IN_CONSULTATION, MEDICAL_RECORD_SAVED, PRESCRIPTION_REVIEWED, PRESCRIPTION_SUBMITTED, COMPLETED)
                            .contains(registration.getStatus()))
                    .count();
            long prescriptionCount = prescriptions.stream()
                    .filter(prescription -> isSameDay(prescription.getCreatedAt(), dayStart, dayEnd))
                    .count();
            long aiCallCount = aiRecords.stream()
                    .filter(record -> isSameDay(record.getCreatedAt(), dayStart, dayEnd))
                    .count();
            points.add(new DashboardTrendPoint(current, registrationCount, visitCount, prescriptionCount, aiCallCount));
        }
        return points;
    }

    @Transactional(readOnly = true)
    public AiUsageStats dashboardAiUsage(ActorContext actorContext, String taskType) {
        List<AICallRecordEntity> records = visibleAiCallRecords(actorContext, visibleRegistrations(actorContext));
        String normalizedTaskType = taskType == null || taskType.isBlank() ? null : taskType.trim().toUpperCase(Locale.ROOT);
        if (normalizedTaskType != null) {
            records = records.stream()
                    .filter(record -> normalizedTaskType.equalsIgnoreCase(record.getTaskType()))
                    .toList();
        }

        long total = records.size();
        long success = records.stream().filter(record -> "COMPLETED".equalsIgnoreCase(record.getCallStatus())).count();
        long failed = records.stream().filter(record -> !"COMPLETED".equalsIgnoreCase(record.getCallStatus())).count();
        long degraded = records.stream().filter(record -> Boolean.TRUE.equals(record.getDegraded())).count();
        long averageDuration = averageDuration(records);
        List<AiUsageBucket> buckets = records.stream()
                .collect(Collectors.groupingBy(record -> firstNonBlank(record.getTaskType(), "UNKNOWN"), LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<AICallRecordEntity> bucketRecords = entry.getValue();
                    long bucketTotal = bucketRecords.size();
                    long bucketSuccess = bucketRecords.stream().filter(record -> "COMPLETED".equalsIgnoreCase(record.getCallStatus())).count();
                    long bucketFailed = bucketRecords.stream().filter(record -> !"COMPLETED".equalsIgnoreCase(record.getCallStatus())).count();
                    long bucketDegraded = bucketRecords.stream().filter(record -> Boolean.TRUE.equals(record.getDegraded())).count();
                    return new AiUsageBucket(
                            entry.getKey(),
                            bucketTotal,
                            bucketSuccess,
                            bucketFailed,
                            bucketDegraded,
                            ratio(bucketTotal, total),
                            averageDuration(bucketRecords)
                    );
                })
                .toList();
        return new AiUsageStats(total, success, failed, degraded, ratio(success, total), averageDuration, buckets, Instant.now());
    }

    @Transactional(readOnly = true)
    public PrescriptionReviewRate dashboardPrescriptionReviewRate(ActorContext actorContext) {
        List<PrescriptionReviewEntity> reviews = visiblePrescriptionReviews(actorContext);
        long total = reviews.size();
        long low = reviews.stream().filter(review -> "LOW".equalsIgnoreCase(review.getRiskLevel())).count();
        long medium = reviews.stream().filter(review -> "MEDIUM".equalsIgnoreCase(review.getRiskLevel())).count();
        long high = reviews.stream().filter(review -> "HIGH".equalsIgnoreCase(review.getRiskLevel())).count();
        long manual = reviews.stream().filter(this::requiresManualReview).count();
        long unknown = reviews.stream().filter(review -> isUnknownRisk(review.getRiskLevel())).count();
        return new PrescriptionReviewRate(total, low, medium, high, manual, unknown, ratio(low, total), Instant.now());
    }

    @Transactional(readOnly = true)
    public RiskDistribution dashboardRiskDistribution(ActorContext actorContext) {
        List<PrescriptionReviewEntity> reviews = visiblePrescriptionReviews(actorContext);
        long total = reviews.size();
        List<RiskDistributionBucket> buckets = List.of("LOW", "MEDIUM", "HIGH", "UNKNOWN").stream()
                .map(level -> {
                    long count = reviews.stream()
                            .filter(review -> "UNKNOWN".equals(level)
                                    ? isUnknownRisk(review.getRiskLevel())
                                    : level.equalsIgnoreCase(review.getRiskLevel()))
                            .count();
                    return new RiskDistributionBucket(level, count, ratio(count, total));
                })
                .toList();
        return new RiskDistribution(total, buckets, Instant.now());
    }

    @Transactional(readOnly = true)
    public TriageAccuracyStats dashboardTriageAccuracy(ActorContext actorContext) {
        List<RegistrationEntity> registrations = visibleRegistrations(actorContext);
        Set<Long> registrationIds = registrations.stream().map(RegistrationEntity::getId).collect(Collectors.toSet());
        List<FeedbackEntity> feedbacks = feedbackRepository.findAll().stream()
                .filter(feedback -> registrationIds.contains(feedback.getRegistrationId()))
                .toList();
        long accurate = feedbacks.stream().filter(feedback -> Boolean.TRUE.equals(feedback.getTriageAccurate())).count();
        long inaccurate = feedbacks.stream().filter(feedback -> Boolean.FALSE.equals(feedback.getTriageAccurate())).count();
        long sample = accurate + inaccurate;
        long noFeedback = Math.max(0, registrations.size() - feedbacks.size());
        return new TriageAccuracyStats(feedbacks.size(), accurate, inaccurate, noFeedback, ratio(accurate, sample), sample, Instant.now());
    }

    @Transactional(readOnly = true)
    public List<TriageResponse> listTriageRecords(ActorContext actorContext) {
        List<TriageRecordEntity> records = actorContext != null && actorContext.isPatient()
                ? triageRecordRepository.findByPatientIdOrderByCreatedAtDesc(actorContext.getRequiredPatientId())
                : triageRecordRepository.findByCallStatusOrderByCreatedAtDesc("COMPLETED");
        Map<String, DepartmentEntity> departments = departmentRepository.findByStatusOrderByNameAsc(ACTIVE).stream()
                .collect(Collectors.toMap(DepartmentEntity::getName, Function.identity(), (left, right) -> left));
        return records.stream()
                .map(record -> {
                    DepartmentEntity department = departments.get(record.getRecommendedDept());
                    List<DoctorOption> doctors = department == null ? List.of() : listDoctors(department.getId());
                    return new TriageResponse(
                            record.getId(),
                            record.getChiefComplaint(),
                            department == null ? null : department.getId(),
                            record.getRecommendedDept(),
                            doctors,
                            department == null ? List.of() : listAvailableSchedules(department.getId()),
                            record.getAiResponseRaw(),
                            record.getCallStatus(),
                            record.getRecommendationSource(),
                            department == null ? null : department.getId(),
                            record.getRecommendedDept(),
                            false
                    );
                }).toList();
    }

    private void requireDashboardActor(ActorContext actorContext) {
        if (actorContext == null || !(actorContext.isDoctor() || actorContext.isAdmin())) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "dashboard permission required");
        }
    }

    private List<RegistrationEntity> visibleRegistrations(ActorContext actorContext) {
        requireDashboardActor(actorContext);
        return actorContext.isDoctor()
                ? registrationRepository.findByDoctorIdOrderByRegistrationTimeDesc(actorContext.getRequiredDoctorId())
                : registrationRepository.findAll();
    }

    private List<PrescriptionReviewEntity> visiblePrescriptionReviews(ActorContext actorContext) {
        requireDashboardActor(actorContext);
        return actorContext.isDoctor()
                ? prescriptionReviewRepository.findByDoctorIdOrderByCreatedAtDesc(actorContext.getRequiredDoctorId())
                : prescriptionReviewRepository.findAll();
    }

    private List<AICallRecordEntity> visibleAiCallRecords(ActorContext actorContext, List<RegistrationEntity> registrations) {
        requireDashboardActor(actorContext);
        if (actorContext.isAdmin()) {
            return aiCallRecordRepository.findAll();
        }
        Set<Long> registrationIds = registrations.stream().map(RegistrationEntity::getId).collect(Collectors.toSet());
        Set<Long> triageRecordIds = registrations.stream()
                .map(RegistrationEntity::getTriageRecordId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> reviewIds = prescriptionReviewRepository.findByDoctorIdOrderByCreatedAtDesc(actorContext.getRequiredDoctorId()).stream()
                .map(PrescriptionReviewEntity::getId)
                .collect(Collectors.toSet());
        return aiCallRecordRepository.findAll().stream()
                .filter(record -> Objects.equals(record.getOperatorId(), actorContext.userId())
                        || ("TRIAGE".equalsIgnoreCase(record.getTaskType()) && triageRecordIds.contains(record.getBusinessRecordId()))
                        || ("MEDICAL_RECORD".equalsIgnoreCase(record.getTaskType()) && registrationIds.contains(record.getBusinessRecordId()))
                        || ("PRESCRIPTION_REVIEW".equalsIgnoreCase(record.getTaskType()) && reviewIds.contains(record.getBusinessRecordId())))
                .toList();
    }

    private void ensureTriageAccuracyFeedback(FeedbackEntity feedback, RegistrationEntity registration) {
        if (feedback == null || registration == null) {
            return;
        }
        if (triageAccuracyFeedbackRepository.findByFeedbackId(feedback.getId()).isPresent()) {
            return;
        }
        TriageRecordEntity triageRecord = registration.getTriageRecordId() == null
                ? null
                : triageRecordRepository.findById(registration.getTriageRecordId()).orElse(null);
        TriageAccuracyFeedbackEntity accuracy = new TriageAccuracyFeedbackEntity();
        accuracy.setFeedbackId(feedback.getId());
        accuracy.setRecommendedDeptSnapshot(triageRecord == null ? null : triageRecord.getRecommendedDept());
        accuracy.setActualDeptSnapshot(firstNonBlank(registration.getDepartmentSnapshot(), null));
        accuracy.setAccuracyLabel(feedback.getTriageAccurate() == null
                ? "UNKNOWN"
                : Boolean.TRUE.equals(feedback.getTriageAccurate()) ? "ACCURATE" : "INACCURATE");
        accuracy.setReasonTags("PATIENT_FEEDBACK");
        accuracy.setNotes(feedback.getComment());
        triageAccuracyFeedbackRepository.save(accuracy);
    }

    private RegistrationEntity requireDoctorRegistration(Long registrationId, Long doctorId) {
        return registrationRepository.findByIdAndDoctorId(registrationId, doctorId)
                .orElseThrow(() -> notFound("registration not found"));
    }

    private Long requirePatient(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isPatient()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "patient permission required");
        }
        return actorContext.getRequiredPatientId();
    }

    private Long requireDoctor(ActorContext actorContext) {
        if (actorContext == null || !actorContext.isDoctor()) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "doctor permission required");
        }
        return actorContext.getRequiredDoctorId();
    }

    private DepartmentEntity pickExpandedDepartment(String complaint) {
        String normalized = complaint == null ? "" : complaint.toLowerCase(Locale.ROOT);
        String code;
        if (containsAny(normalized, "胸痛", "胸闷", "心悸", "心慌", "血压高", "高血压", "血压异常", "心绞痛", "心衰", "心律失常")) {
            code = "cardiology";
        } else if (containsAny(normalized, "头痛", "眩晕", "抽搐", "癫痫", "中风", "麻木", "失眠", "记忆", "帕金森", "脑梗", "脑出血")) {
            code = "neurology";
        } else if (containsAny(normalized, "骨折", "关节痛", "腰痛", "腰腿痛", "扭伤", "颈椎", "脊柱", "骨痛", "运动损伤", "肩痛")) {
            code = "orthopedics";
        } else if (containsAny(normalized, "皮疹", "过敏", "皮肤", "瘙痒", "痤疮", "湿疹", "荨麻疹", "银屑病", "真菌感染", "脱发")) {
            code = "dermatology";
        } else if (containsAny(normalized, "儿童", "小孩", "孩子", "宝宝", "婴儿", "儿科", "生长发育", "疫苗", "发育迟缓")) {
            code = "pediatrics";
        } else if (containsAny(normalized, "咳嗽", "咳痰", "气短", "喘", "喘息", "呼吸困难", "肺炎", "慢阻肺", "支气管", "哮喘")) {
            code = "respiratory-medicine";
        } else if (containsAny(normalized, "腹痛", "腹泻", "腹胀", "反酸", "胃痛", "胃胀", "恶心", "呕吐", "便血", "黑便", "消化不良")) {
            code = "gastroenterology";
        } else if (containsAny(normalized, "糖尿病", "甲状腺", "血糖", "血脂", "肥胖", "多饮", "多尿", "消瘦", "代谢", "内分泌")) {
            code = "endocrinology";
        } else if (containsAny(normalized, "耳鸣", "听力下降", "耳痛", "鼻塞", "流涕", "咽痛", "声音嘶哑", "喉咙痛", "鼻窦炎", "扁桃体")) {
            code = "otolaryngology";
        } else if (containsAny(normalized, "视力下降", "视物模糊", "眼红", "眼痛", "干眼", "流泪", "结膜炎", "白内障", "青光眼")) {
            code = "ophthalmology";
        } else if (containsAny(normalized, "尿频", "尿急", "尿痛", "血尿", "排尿", "结石", "前列腺", "腰酸", "肾绞痛", "泌尿")) {
            code = "urology";
        } else if (containsAny(normalized, "月经", "白带", "阴道", "盆腔", "妇科", "妊娠", "怀孕", "孕产", "阴道出血", "经期")) {
            code = "gynecology";
        } else {
            code = "internal-medicine";
        }
        return departmentRepository.findByCode(code)
                .or(() -> departmentRepository.findByStatusOrderByNameAsc(ACTIVE).stream().findFirst())
                .orElseThrow(() -> notFound("no active department configured"));
    }

    private DepartmentEntity pickDepartment(String complaint) {
        return pickExpandedDepartment(complaint);
    }

    private Optional<DepartmentEntity> resolveActiveDepartment(String code, String name) {
        String normalizedCode = blankToNull(code);
        if (normalizedCode != null) {
            Optional<DepartmentEntity> byCode = departmentRepository.findByCode(normalizedCode)
                    .filter(department -> ACTIVE.equals(department.getStatus()));
            if (byCode.isPresent()) {
                return byCode;
            }
        }

        String normalizedName = blankToNull(name);
        if (normalizedName == null) {
            return Optional.empty();
        }
        List<DepartmentEntity> activeDepartments = departmentRepository.findByStatusOrderByNameAsc(ACTIVE);
        Optional<DepartmentEntity> exactMatch = activeDepartments.stream()
                .filter(department -> department.getName().equals(normalizedName)
                        || department.getCode().equalsIgnoreCase(normalizedName))
                .findFirst();
        if (exactMatch.isPresent()) {
            return exactMatch;
        }
        return activeDepartments.stream()
                .filter(department -> normalizedName.contains(department.getName()))
                .findFirst();
    }

    private boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String buildTriageReason(String complaint, DepartmentEntity department, List<DoctorOption> doctors) {
        String doctorText = doctors.isEmpty()
                ? "暂无可推荐医生，请手动选择号源。"
                : "推荐医生：" + doctors.stream().map(DoctorOption::name).collect(Collectors.joining("、")) + "。";
        return "根据主诉“" + complaint + "”，本地分诊规则建议优先前往" + department.getName()
                + "。理由：症状关键词与该科室常见就诊范围匹配。" + doctorText;
    }

    private AIModels.AIExecutionOutcome<String> invokeAi(String taskType,
                                                         String deptCode,
                                                         Map<String, String> variables,
                                                         List<AiContentAttachment> attachments,
                                                         String fallbackText,
                                                         boolean stream,
                                                         Consumer<String> chunkConsumer) {
        return invokeAi(taskType, deptCode, variables, attachments, fallbackText, stream, chunkConsumer, null);
    }

    private AIModels.AIExecutionOutcome<String> invokeAi(String taskType,
                                                         String deptCode,
                                                         Map<String, String> variables,
                                                         List<AiContentAttachment> attachments,
                                                         String fallbackText,
                                                         boolean stream,
                                                         Consumer<String> chunkConsumer,
                                                         Consumer<String> thinkingConsumer) {
        List<AIModels.AIContentPart> parts = attachments == null
                ? List.of()
                : attachments.stream().map(this::toContentPart).toList();
        return aiInvocationService.chat(taskType, deptCode, variables, parts, fallbackText, stream, chunkConsumer, thinkingConsumer);
    }

    private AIModels.AIContentPart toContentPart(AiContentAttachment attachment) {
        if (attachment == null) {
            return AIModels.AIContentPart.text("");
        }
        String type = firstNonBlank(attachment.type(), "text").toLowerCase(Locale.ROOT);
        return switch (type) {
            case "image", "image_url" -> AIModels.AIContentPart.imageUrl(attachment.url(), firstNonBlank(attachment.detail(), "auto"));
            case "video", "video_url" -> AIModels.AIContentPart.videoUrl(attachment.url());
            case "audio", "input_audio" -> AIModels.AIContentPart.inputAudio(attachment.data(), firstNonBlank(attachment.mimeType(), "mp3"));
            case "file", "file_url" -> AIModels.AIContentPart.fileUrl(attachment.url(), attachment.mimeType(), attachment.name());
            default -> AIModels.AIContentPart.text(firstNonBlank(firstNonBlank(attachment.data(), attachment.url()), attachment.name()));
        };
    }

    private String departmentNameByRegistration(RegistrationEntity registration) {
        if (registration == null) {
            return null;
        }
        return departmentRepository.findById(registration.getDepartmentId())
                .map(DepartmentEntity::getName)
                .orElse(registration.getDepartmentSnapshot());
    }

    private String departmentCodeByRegistration(RegistrationEntity registration) {
        if (registration == null) {
            return null;
        }
        return departmentRepository.findById(registration.getDepartmentId())
                .map(DepartmentEntity::getCode)
                .orElse(null);
    }

    private String buildMedicalRecordFallbackText(String chief,
                                                  String conversationText,
                                                  PatientEntity patient,
                                                  String diagnosis) {
        String resolvedDiagnosis = firstNonBlank(diagnosis, inferDiagnosis(conversationText));
        return String.join("\n",
                "chiefComplaint: " + firstNonBlank(chief, firstSentence(conversationText)),
                "presentIllness: " + firstNonBlank(conversationText, ""),
                "pastHistory: " + firstNonBlank(patient.getMedicalHistory(), "未见特殊既往史"),
                "physicalExam: 生命体征平稳，建议医生结合查体补充。",
                "preliminaryDiagnosis: " + resolvedDiagnosis,
                "treatmentPlan: " + buildTreatmentPlan(resolvedDiagnosis),
                "docNote: 本地规则已生成病历草稿，请医生确认。"
        );
    }

    private MedicalRecordDraft parseMedicalRecordDraft(String text) {
        Map<String, String> values = AITextParser.parseKeyValueBlock(text);
        return new MedicalRecordDraft(
                AITextParser.firstNonBlank(values, "chiefComplaint", null),
                AITextParser.firstNonBlank(values, "presentIllness", null),
                AITextParser.firstNonBlank(values, "pastHistory", null),
                AITextParser.firstNonBlank(values, "physicalExam", null),
                AITextParser.firstNonBlank(values, "preliminaryDiagnosis", null),
                AITextParser.firstNonBlank(values, "treatmentPlan", null),
                AITextParser.firstNonBlank(values, "docNote", null)
        );
    }

    private record MedicalRecordDraft(
            String chiefComplaint,
            String presentIllness,
            String pastHistory,
            String physicalExam,
            String preliminaryDiagnosis,
            String treatmentPlan,
            String docNote
    ) {
    }

    private ReviewComputation computeReview(RegistrationEntity registration,
                                            List<PrescriptionItemRequest> items,
                                            List<DrugEntity> drugs) {
        PatientEntity patient = patientRepository.findById(registration.getPatientId()).orElseThrow();
        List<String> hits = new ArrayList<>();
        String risk = "LOW";
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            PrescriptionItemRequest item = items.get(i);
            DrugEntity drug = drugs.get(i);
            if (!seen.add(item.drugId())) {
                hits.add("重复用药：" + drug.getName() + " 在处方中出现多次。");
                risk = maxRisk(risk, "MEDIUM");
            }
            if (patient.getAllergyHistory() != null
                    && !patient.getAllergyHistory().contains("无")
                    && drug.getContraindications() != null
                    && drug.getContraindications().contains(patient.getAllergyHistory())) {
                hits.add("过敏风险：" + patient.getAllergyHistory() + " 与 " + drug.getName() + " 禁忌信息相关。");
                risk = maxRisk(risk, "HIGH");
            }
            if (item.quantity() != null && item.quantity() > 30) {
                hits.add("数量偏高：" + drug.getName() + " 数量超过 30，请确认疗程。");
                risk = maxRisk(risk, "MEDIUM");
            }
            if (item.dosage() != null && item.dosage().compareTo(new BigDecimal("2.00")) > 0) {
                hits.add("剂量提醒：" + drug.getName() + " 单次剂量较高，请结合说明书。");
                risk = maxRisk(risk, "MEDIUM");
            }
        }
        for (PrescriptionRuleDefinitionEntity rule : ruleRepository.findByStatusOrderByRuleCodeAsc(ACTIVE)) {
            for (DrugEntity drug : drugs) {
                if (rule.getApplicableDrugs() != null && rule.getApplicableDrugs().contains(drug.getName())) {
                    hits.add(rule.getAlertMessage() == null ? "命中规则：" + rule.getRuleCode() : rule.getAlertMessage());
                    risk = maxRisk(risk, firstNonBlank(rule.getRiskLevel(), "MEDIUM"));
                }
            }
        }
        if (hits.isEmpty()) {
            hits.add("未命中高风险本地规则，仍需医生结合患者情况确认。");
        }
        String missing = medicalRecordRepository.findFirstByRegistrationIdOrderByVersionDesc(registration.getId()).isPresent()
                ? ""
                : "缺少已保存病历上下文";
        String summary = switch (risk) {
            case "HIGH" -> "本地规则判断存在高风险，建议调整处方或补充人工确认。";
            case "MEDIUM" -> "本地规则判断存在中等风险，建议医生复核剂量、疗程或禁忌。";
            default -> "本地规则未发现明显风险，建议常规复核后提交。";
        };
        return new ReviewComputation(risk, String.join("\n", hits), missing, summary, summary);
    }

    private String maxRisk(String current, String candidate) {
        return riskRank(candidate) > riskRank(current) ? candidate : current;
    }

    private int riskRank(String risk) {
        return switch (risk) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 2;
            case "LOW" -> 1;
            default -> 0;
        };
    }

    private PrescriptionItemEntity toPrescriptionItem(Long prescriptionId, PrescriptionItemRequest request, DrugEntity drug) {
        PrescriptionItemEntity item = new PrescriptionItemEntity();
        item.setPrescriptionId(prescriptionId);
        item.setDrugId(drug.getId());
        item.setDrugName(drug.getName());
        item.setSpecification(drug.getSpecification());
        item.setDosageForm(drug.getDosageForm());
        item.setPackageUnit(drug.getPackageUnit());
        item.setManufacturer(drug.getManufacturer());
        item.setUnitPrice(drug.getUnitPrice());
        item.setDefaultUsage(drug.getDefaultUsage());
        item.setDosage(request.dosage());
        item.setFrequency(request.frequency());
        item.setDuration(request.duration());
        item.setQuantity(request.quantity());
        item.setUsageInstruction(request.usageInstruction());
        return item;
    }

    private AICallRecordEntity aiCallRecord(String taskType,
                                            ActorContext actorContext,
                                            String input,
                                            String output,
                                            long started) {
        AICallRecordEntity entity = new AICallRecordEntity();
        entity.setTaskType(taskType);
        entity.setOperatorId(actorContext == null ? null : actorContext.userId());
        entity.setOperatorRole(actorContext == null || actorContext.role() == null ? null : actorContext.role().name());
        entity.setProvider("LOCAL_RULE");
        entity.setModelName("local-simulator");
        entity.setConfigVersion("local-v1");
        entity.setPromptVersion("local-v1");
        entity.setRequestId(UUID.randomUUID().toString());
        entity.setInputSummary(input);
        entity.setOutputSummary(output);
        entity.setCallStatus("COMPLETED");
        entity.setDurationMs(Math.max(1L, System.currentTimeMillis() - started));
        entity.setTraceId(UUID.randomUUID().toString());
        entity.setDegraded(false);
        entity.setRetryCount(0);
        return entity;
    }

    private AICallRecordEntity aiCallRecord(String taskType,
                                            ActorContext actorContext,
                                            String input,
                                            String output,
                                            long started,
                                            AIModels.AIInvocationMeta meta) {
        AICallRecordEntity entity = aiCallRecord(taskType, actorContext, input, output, started);
        if (meta != null) {
            entity.setProvider(firstNonBlank(meta.provider(), entity.getProvider()));
            entity.setModelName(firstNonBlank(meta.modelName(), entity.getModelName()));
            entity.setConfigVersion(firstNonBlank(meta.configVersion(), entity.getConfigVersion()));
            entity.setPromptVersion(firstNonBlank(meta.promptVersion(), entity.getPromptVersion()));
            entity.setRequestId(firstNonBlank(meta.requestId(), entity.getRequestId()));
            entity.setOutputSummary(firstNonBlank(meta.responseText(), entity.getOutputSummary()));
            entity.setCallStatus(firstNonBlank(meta.callStatus(), entity.getCallStatus()));
            entity.setErrorSummary(blankToNull(meta.errorSummary()));
            entity.setDurationMs(meta.durationMs() > 0 ? meta.durationMs() : entity.getDurationMs());
            entity.setTraceId(firstNonBlank(meta.traceId(), entity.getTraceId()));
            entity.setDegraded(meta.degraded());
        }
        return entity;
    }

    private RegistrationSummary toRegistrationSummary(RegistrationEntity registration) {
        PatientEntity patient = patientRepository.findById(registration.getPatientId()).orElse(null);
        DoctorEntity doctor = doctorRepository.findById(registration.getDoctorId()).orElse(null);
        DepartmentEntity department = departmentRepository.findById(registration.getDepartmentId()).orElse(null);
        ScheduleEntity schedule = scheduleRepository.findById(registration.getScheduleId()).orElse(null);
        TriageRecordEntity triage = registration.getTriageRecordId() == null
                ? null
                : triageRecordRepository.findById(registration.getTriageRecordId()).orElse(null);
        MedicalRecordEntity medicalRecord = medicalRecordRepository.findFirstByRegistrationIdOrderByVersionDesc(registration.getId()).orElse(null);
        PrescriptionEntity prescription = prescriptionRepository.findByRegistrationIdOrderByCreatedAtDesc(registration.getId()).stream()
                .findFirst()
                .orElse(null);
        return new RegistrationSummary(
                registration.getId(),
                registration.getPatientId(),
                patient == null ? null : patient.getName(),
                registration.getDoctorId(),
                doctor == null ? registration.getDoctorSnapshot() : doctor.getName(),
                registration.getDepartmentId(),
                department == null ? registration.getDepartmentSnapshot() : department.getName(),
                registration.getScheduleId(),
                schedule == null ? null : schedule.getWorkDate(),
                schedule == null ? null : schedule.getPeriod(),
                registration.getVisitLevelSnapshot(),
                registration.getStatus(),
                registration.getTriageRecordId(),
                triage == null ? null : triage.getChiefComplaint(),
                registration.getRegistrationTime(),
                registration.getConsultationStartTime(),
                registration.getRecordConfirmedTime(),
                registration.getPrescriptionSubmittedTime(),
                registration.getCompletedTime(),
                medicalRecord == null ? null : medicalRecord.getId(),
                prescription == null ? null : prescription.getId(),
                prescription == null ? null : prescription.getRiskLevel()
        );
    }

    private MedicalRecordSummary toMedicalRecordSummary(MedicalRecordEntity record) {
        return toMedicalRecordSummary(record, false);
    }

    private MedicalRecordSummary toMedicalRecordSummary(MedicalRecordEntity record, boolean degraded) {
        RegistrationEntity registration = record.getRegistrationId() == null ? null : registrationRepository.findById(record.getRegistrationId()).orElse(null);
        PatientEntity patient = record.getPatientId() == null ? null : patientRepository.findById(record.getPatientId()).orElse(null);
        DoctorEntity doctor = record.getDoctorId() == null ? null : doctorRepository.findById(record.getDoctorId()).orElse(null);
        DepartmentEntity department = registration == null ? null : departmentRepository.findById(registration.getDepartmentId()).orElse(null);
        return new MedicalRecordSummary(
                record.getId(),
                record.getRegistrationId(),
                record.getPatientId(),
                patient == null ? null : patient.getName(),
                record.getDoctorId(),
                doctor == null ? null : doctor.getName(),
                department == null ? null : department.getName(),
                record.getChiefComplaint(),
                record.getPresentIllness(),
                record.getPastHistory(),
                record.getPhysicalExam(),
                record.getPreliminaryDiagnosis(),
                record.getTreatmentPlan(),
                record.getConversationText(),
                record.getDocNote(),
                record.getAiGenerated(),
                record.getVersion(),
                record.getCreatedAt(),
                degraded
        );
    }

    private PrescriptionSummary toPrescriptionSummary(PrescriptionEntity prescription) {
        RegistrationEntity registration = registrationRepository.findById(prescription.getRegistrationId()).orElse(null);
        PatientEntity patient = patientRepository.findById(prescription.getPatientId()).orElse(null);
        DoctorEntity doctor = doctorRepository.findById(prescription.getDoctorId()).orElse(null);
        DepartmentEntity department = registration == null ? null : departmentRepository.findById(registration.getDepartmentId()).orElse(null);
        List<PrescriptionItemSummary> items = prescriptionItemRepository
                .findByPrescriptionIdOrderByCreatedAtAsc(prescription.getId())
                .stream()
                .map(this::toPrescriptionItemSummary)
                .toList();
        PrescriptionReviewEntity review = prescription.getReviewId() == null
                ? null
                : prescriptionReviewRepository.findById(prescription.getReviewId()).orElse(null);
        return new PrescriptionSummary(
                prescription.getId(),
                prescription.getRegistrationId(),
                prescription.getPatientId(),
                patient == null ? null : patient.getName(),
                prescription.getDoctorId(),
                doctor == null ? null : doctor.getName(),
                department == null ? null : department.getName(),
                prescription.getStatus(),
                prescription.getRiskLevel(),
                prescription.getReviewId(),
                items,
                review == null ? null : toReviewResponse(review, prescription.getId(), items),
                prescription.getCreatedAt()
        );
    }

    private PrescriptionReviewResponse toReviewResponse(PrescriptionReviewEntity review,
                                                        Long prescriptionId,
                                                        List<PrescriptionItemSummary> items) {
        String bindStatus = firstNonBlank(review.getBindStatus(), "UNBOUND");
        String reviewStatus = "BOUND".equalsIgnoreCase(bindStatus) ? "BOUND" : "REVIEWED";
        String ruleEngineStatus = normalizeRuleEngineStatus(review.getRuleEngineStatus(), review.getContextMissingItems());
        String llmCallStatus = firstNonBlank(review.getLlmCallStatus(), "LOCAL_SIMULATED");
        return new PrescriptionReviewResponse(
                review.getId(),
                review.getRegistrationId(),
                prescriptionId == null ? review.getPrescriptionId() : prescriptionId,
                reviewStatus,
                review.getRiskLevel(),
                review.getLocalRuleHits(),
                ruleEngineStatus,
                review.getContextMissingItems(),
                review.getLlmSuggestion(),
                review.getLlmSummary(),
                llmCallStatus,
                review.getPrescriptionSnapshotHash(),
                review.getReviewContextHash(),
                false,
                bindStatus,
                items
        );
    }

    private DiagnosisSuggestionRecordEntity requireDoctorDiagnosisSuggestion(Long suggestionId, Long doctorId) {
        DiagnosisSuggestionRecordEntity entity = diagnosisSuggestionRepository.findById(suggestionId)
                .orElseThrow(() -> notFound("diagnosis suggestion not found"));
        if (!Objects.equals(entity.getDoctorId(), doctorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN.value(), "diagnosis suggestion permission required");
        }
        return entity;
    }

    private DiagnosisSuggestionResponse toDiagnosisSuggestionResponse(DiagnosisSuggestionRecordEntity entity,
                                                                      String summary,
                                                                      boolean degraded) {
        return new DiagnosisSuggestionResponse(
                entity.getId(),
                entity.getRegistrationId(),
                entity.getSuggestedDiagnoses(),
                entity.getSuggestedExamItems(),
                entity.getAdoptionStatus(),
                summary,
                degraded
        );
    }

    private List<PrescriptionItemSummary> prescriptionItemsFor(Long prescriptionId) {
        if (prescriptionId == null) {
            return List.of();
        }
        return prescriptionItemRepository.findByPrescriptionIdOrderByCreatedAtAsc(prescriptionId).stream()
                .map(this::toPrescriptionItemSummary)
                .toList();
    }

    private List<String> nextActions(RegistrationEntity registration) {
        if (registration == null || registration.getStatus() == null) {
            return List.of("等待接诊");
        }
        return switch (registration.getStatus()) {
            case WAITING -> List.of("开始接诊", "查看分诊建议", "查看挂号信息");
            case IN_CONSULTATION -> List.of("录入问诊", "生成病历草稿", "保存正式病历");
            case MEDICAL_RECORD_SAVED -> List.of("开立处方", "处方审核", "结束就诊");
            case PRESCRIPTION_REVIEWED -> List.of("提交处方", "查看审核结果", "结束就诊");
            case PRESCRIPTION_SUBMITTED -> List.of("查看已提交处方", "结束就诊");
            case COMPLETED -> List.of("查看历史记录", "提交反馈");
            case CANCELLED -> List.of("查看取消记录");
            default -> List.of("查看挂号详情");
        };
    }

    private boolean isSameDay(java.time.Instant instant, LocalDateTime dayStart, LocalDateTime dayEnd) {
        if (instant == null) {
            return false;
        }
        LocalDateTime value = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        return !value.isBefore(dayStart) && value.isBefore(dayEnd);
    }

    private boolean isBetween(LocalDateTime value, LocalDateTime start, LocalDateTime end) {
        return value != null && !value.isBefore(start) && value.isBefore(end);
    }

    private double ratio(long part, long total) {
        if (total <= 0) {
            return 0D;
        }
        return Math.round((part * 10000D / total)) / 100D;
    }

    private long averageDuration(List<AICallRecordEntity> records) {
        return Math.round(records.stream()
                .map(AICallRecordEntity::getDurationMs)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average()
                .orElse(0D));
    }

    private boolean requiresManualReview(PrescriptionReviewEntity review) {
        if (review == null) {
            return false;
        }
        String riskLevel = firstNonBlank(review.getRiskLevel(), "UNKNOWN").toUpperCase(Locale.ROOT);
        return "MEDIUM".equals(riskLevel)
                || "HIGH".equals(riskLevel)
                || !firstNonBlank(review.getManualConfirmation(), "").isBlank()
                || !firstNonBlank(review.getContextMissingItems(), "").isBlank()
                || "CONTEXT_MISSING".equalsIgnoreCase(firstNonBlank(review.getRuleEngineStatus(), ""));
    }

    private boolean isUnknownRisk(String riskLevel) {
        return riskLevel == null || riskLevel.isBlank() || "UNKNOWN".equalsIgnoreCase(riskLevel);
    }

    private boolean containsNormalized(String source, String keyword) {
        if (source == null || keyword == null || keyword.isBlank()) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String normalizeRuleEngineStatus(String status, String missingItems) {
        if (status != null && !status.isBlank()) {
            String normalized = status.trim().toUpperCase(Locale.ROOT);
            if ("COMPLETED".equals(normalized)) {
                return "SUCCESS";
            }
            return normalized;
        }
        return missingItems == null || missingItems.isBlank() ? "SUCCESS" : "CONTEXT_MISSING";
    }

    private void upsertPrescriptionReviewNotification(RegistrationEntity registration, PrescriptionReviewEntity review) {
        String alertType = determineNotificationAlertType(review);
        if (alertType == null) {
            return;
        }
        Long businessRecordId = registration.getId();
        NotificationRecordEntity notification = notificationRecordRepository
                .findFirstByRecipientIdAndRecipientRoleAndAlertTypeAndBusinessRecordIdAndReadFalseOrderByCreatedAtDesc(
                        review.getDoctorId(),
                        "DOCTOR",
                        alertType,
                        businessRecordId
                )
                .orElseGet(NotificationRecordEntity::new);
        notification.setRecipientId(review.getDoctorId());
        notification.setRecipientRole("DOCTOR");
        notification.setAlertType(alertType);
        notification.setStatisticsBucket(firstNonBlank(review.getRiskLevel(), "UNKNOWN"));
        notification.setDisplayLevel(determineNotificationDisplayLevel(review));
        notification.setBusinessRecordId(businessRecordId);
        notification.setPatientSummary(buildPatientSummary(registration));
        notification.setRiskSummary(buildRiskSummary(review));
        notification.setRead(false);
        notification = notificationRecordRepository.save(notification);
        notificationWebSocketHandler.publish(toNotificationRecordSummary(notification));
    }

    private String determineNotificationAlertType(PrescriptionReviewEntity review) {
        if (review == null) {
            return null;
        }
        String riskLevel = firstNonBlank(review.getRiskLevel(), "LOW").toUpperCase(Locale.ROOT);
        String missingItems = firstNonBlank(review.getContextMissingItems(), "");
        String ruleEngineStatus = firstNonBlank(review.getRuleEngineStatus(), "");
        if ("HIGH".equals(riskLevel)) {
            return "HIGH_RISK_PRESCRIPTION";
        }
        if ("MEDIUM".equals(riskLevel) || !missingItems.isBlank() || "CONTEXT_MISSING".equalsIgnoreCase(ruleEngineStatus)) {
            return "RISK_UNCERTAIN_MANUAL_REQUIRED";
        }
        return null;
    }

    private String determineNotificationDisplayLevel(PrescriptionReviewEntity review) {
        String riskLevel = firstNonBlank(review.getRiskLevel(), "LOW").toUpperCase(Locale.ROOT);
        if ("HIGH".equals(riskLevel)) {
            return "HIGH";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return "MEDIUM";
        }
        return "MEDIUM";
    }

    private String buildPatientSummary(RegistrationEntity registration) {
        PatientEntity patient = patientRepository.findById(registration.getPatientId()).orElse(null);
        String patientName = patient == null ? "匿名患者" : maskName(patient.getName());
        return "患者：" + patientName
                + "；科室：" + firstNonBlank(registration.getDepartmentSnapshot(), "未分科")
                + "；医生：" + firstNonBlank(registration.getDoctorSnapshot(), "未指定");
    }

    private String buildRiskSummary(PrescriptionReviewEntity review) {
        StringBuilder builder = new StringBuilder();
        builder.append("风险等级：").append(firstNonBlank(review.getRiskLevel(), "UNKNOWN"));
        if (review.getLocalRuleHits() != null && !review.getLocalRuleHits().isBlank()) {
            builder.append("；命中：").append(truncateText(review.getLocalRuleHits(), 120));
        }
        if (review.getLlmSummary() != null && !review.getLlmSummary().isBlank()) {
            builder.append("；建议：").append(truncateText(review.getLlmSummary(), 120));
        }
        return builder.toString();
    }

    private String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "匿名患者";
        }
        String normalized = name.trim();
        if (normalized.length() == 1) {
            return normalized + "*";
        }
        return normalized.charAt(0) + "**";
    }

    private String truncateText(String text, int length) {
        if (text == null) {
            return "";
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= length) {
            return normalized;
        }
        return normalized.substring(0, length) + "...";
    }

    private boolean canViewNotification(ActorContext actorContext, NotificationRecordEntity notification) {
        if (actorContext == null || notification == null) {
            return false;
        }
        if (actorContext.isAdmin()) {
            return true;
        }
        if (actorContext.isDoctor()) {
            return Objects.equals(notification.getRecipientId(), actorContext.doctorId())
                    && "DOCTOR".equalsIgnoreCase(notification.getRecipientRole());
        }
        return actorContext.isPatient()
                && Objects.equals(notification.getRecipientId(), actorContext.patientId())
                && "PATIENT".equalsIgnoreCase(notification.getRecipientRole());
    }

    private List<PrescriptionItemSummary> toItemSummariesFromRequest(List<PrescriptionItemRequest> requests, List<DrugEntity> drugs) {
        List<PrescriptionItemSummary> items = new ArrayList<>();
        for (int i = 0; i < requests.size(); i++) {
            PrescriptionItemRequest request = requests.get(i);
            DrugEntity drug = drugs.get(i);
            items.add(new PrescriptionItemSummary(
                    null,
                    drug.getId(),
                    drug.getName(),
                    drug.getSpecification(),
                    drug.getDosageForm(),
                    drug.getPackageUnit(),
                    drug.getUnitPrice(),
                    request.dosage(),
                    request.frequency(),
                    request.duration(),
                    request.quantity(),
                    request.usageInstruction()
            ));
        }
        return items;
    }

    private PrescriptionItemSummary toPrescriptionItemSummary(PrescriptionItemEntity item) {
        return new PrescriptionItemSummary(
                item.getId(),
                item.getDrugId(),
                item.getDrugName(),
                item.getSpecification(),
                item.getDosageForm(),
                item.getPackageUnit(),
                item.getUnitPrice(),
                item.getDosage(),
                item.getFrequency(),
                item.getDuration(),
                item.getQuantity(),
                item.getUsageInstruction()
        );
    }

    private NotificationRecordSummary toNotificationRecordSummary(NotificationRecordEntity entity) {
        return new NotificationRecordSummary(
                entity.getId(),
                entity.getRecipientId(),
                entity.getRecipientRole(),
                entity.getAlertType(),
                entity.getStatisticsBucket(),
                entity.getDisplayLevel(),
                entity.getBusinessRecordId(),
                entity.getPatientSummary(),
                entity.getRiskSummary(),
                entity.getRead(),
                entity.getCreatedAt()
        );
    }

    private FeedbackResponse toFeedbackResponse(FeedbackEntity feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getRegistrationId(),
                feedback.getRating(),
                feedback.getTriageAccurate(),
                feedback.getComment(),
                feedback.getCreatedAt()
        );
    }

    private DepartmentOption toDepartmentOption(DepartmentEntity entity) {
        return new DepartmentOption(entity.getId(), entity.getCode(), entity.getName(), entity.getType(), entity.getDescription());
    }

    private DoctorOption toDoctorOption(DoctorEntity doctor, DepartmentEntity department) {
        return new DoctorOption(
                doctor.getId(),
                doctor.getUsername(),
                doctor.getName(),
                doctor.getDepartmentId(),
                department == null ? null : department.getName(),
                doctor.getTitle(),
                doctor.getSpecialty(),
                doctor.getIntroduction()
        );
    }

    private ScheduleOption toScheduleOption(ScheduleEntity schedule, DoctorEntity doctor, DepartmentEntity department) {
        return new ScheduleOption(
                schedule.getId(),
                schedule.getDoctorId(),
                doctor == null ? null : doctor.getName(),
                schedule.getDepartmentId(),
                department == null ? null : department.getName(),
                schedule.getWorkDate(),
                schedule.getPeriod(),
                schedule.getTotalSlots(),
                schedule.getRemainingSlots(),
                schedule.getVisitLevel(),
                schedule.getStatus()
        );
    }

    private DrugOption toDrugOption(DrugEntity drug) {
        return new DrugOption(
                drug.getId(),
                drug.getCode(),
                drug.getName(),
                drug.getPinyinCode(),
                drug.getSpecification(),
                drug.getDosageForm(),
                drug.getPackageUnit(),
                drug.getManufacturer(),
                drug.getUnitPrice(),
                drug.getDefaultUsage(),
                drug.getContraindications(),
                drug.getIndications()
        );
    }

    private Map<Long, DepartmentEntity> departmentsById() {
        return departmentRepository.findAll().stream().collect(Collectors.toMap(DepartmentEntity::getId, Function.identity()));
    }

    private Map<Long, DoctorEntity> doctorsById() {
        return doctorRepository.findAll().stream().collect(Collectors.toMap(DoctorEntity::getId, Function.identity()));
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstSentence(String text) {
        if (text == null || text.isBlank()) {
            return "主诉待补充";
        }
        String normalized = text.trim();
        int index = -1;
        for (String delimiter : List.of("\n", "\r", ".", "。", "?", "？", ";", "；", "!", "！")) {
            int found = normalized.indexOf(delimiter);
            if (found >= 0 && (index < 0 || found < index)) {
                index = found;
            }
        }
        return index >= 0 ? normalized.substring(0, index + 1) : normalized;
    }

    private String inferDiagnosis(String text) {
        String value = text == null ? "" : text;
        if (containsAny(value, "胸", "心悸", "气短", "血压")) {
            return "胸痛待查，需排除心血管相关疾病";
        }
        if (containsAny(value, "咳", "发热", "咽", "呼吸")) {
            return "上呼吸道感染可能";
        }
        if (containsAny(value, "胃", "腹", "恶心", "腹泻")) {
            return "胃肠功能紊乱可能";
        }
        return "常见内科不适，需结合查体进一步明确";
    }

    private String buildTreatmentPlan(String diagnosis) {
        if (diagnosis.contains("心")) {
            return "建议完善心电图、心肌酶和血压监测；如症状加重及时急诊。";
        }
        if (diagnosis.contains("呼吸")) {
            return "建议休息、补液，必要时完善血常规和胸部影像检查。";
        }
        return "建议完善基础检查，结合症状给予对症处理并复诊。";
    }

    private String suggestExamItems(String diagnosis) {
        if (diagnosis.contains("心")) {
            return "心电图、心肌酶谱、血压监测、必要时心脏彩超";
        }
        if (diagnosis.contains("呼吸")) {
            return "血常规、C 反应蛋白、胸部影像";
        }
        if (diagnosis.contains("胃") || diagnosis.contains("肠")) {
            return "腹部查体、便常规、必要时腹部超声";
        }
        return "血常规、尿常规、基础生化";
    }

    private String hashItems(List<PrescriptionItemRequest> items) {
        return sha256(items.stream()
                .map(item -> item.drugId() + ":" + normalizeDosage(item.dosage()) + ":" + normalizeStr(item.frequency()) + ":" + normalizeStr(item.duration()) + ":" + item.quantity())
                .collect(Collectors.joining("|")));
    }

    private String normalizeStr(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeDosage(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }

    private String hashContext(RegistrationEntity registration) {
        String record = medicalRecordRepository.findFirstByRegistrationIdOrderByVersionDesc(registration.getId())
                .map(medical -> firstNonBlank(medical.getPreliminaryDiagnosis(), "") + ":" + medical.getVersion())
                .orElse("NO_RECORD");
        return sha256(registration.getId() + ":" + registration.getPatientId() + ":" + record);
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : encoded) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND.value(), message);
    }

    private ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST.value(), message);
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT.value(), message);
    }

    private List<PrescriptionItemRequest> requirePrescriptionItems(List<PrescriptionItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw badRequest("please add at least one prescription item before review or submit");
        }
        return items;
    }

    private record ReviewComputation(
            String riskLevel,
            String localRuleHits,
            String contextMissingItems,
            String suggestion,
            String summary
    ) {
    }
}


