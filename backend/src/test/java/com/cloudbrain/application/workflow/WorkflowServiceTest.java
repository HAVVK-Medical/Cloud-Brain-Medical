package com.cloudbrain.application.workflow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudbrain.application.ai.AIInvocationService;
import com.cloudbrain.application.ai.AIModels;
import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiContentAttachment;
import com.cloudbrain.dto.workflow.WorkflowDtos.ConversationTriageConfirmRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionAdoptRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionIgnoreRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.DiagnosisSuggestionRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.FeedbackCreateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordGenerateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordSaveRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionItemRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionReviewRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionSubmitRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationCancelRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationCreateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.TriageRequest;
import com.cloudbrain.entity.auth.DoctorEntity;
import com.cloudbrain.entity.auth.PatientEntity;
import com.cloudbrain.entity.core.AICallRecordEntity;
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
import com.cloudbrain.security.ActorRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class WorkflowServiceTest {

    private DepartmentJpaRepository departments;
    private DoctorJpaRepository doctors;
    private PatientJpaRepository patients;
    private ScheduleJpaRepository schedules;
    private TriageRecordJpaRepository triageRecords;
    private RegistrationJpaRepository registrations;
    private ConsultationNoteJpaRepository notes;
    private MedicalRecordJpaRepository medicalRecords;
    private DiagnosisSuggestionRecordJpaRepository diagnosisSuggestions;
    private DrugJpaRepository drugs;
    private PrescriptionJpaRepository prescriptions;
    private PrescriptionItemJpaRepository prescriptionItems;
    private PrescriptionReviewJpaRepository prescriptionReviews;
    private PrescriptionRuleDefinitionJpaRepository rules;
    private AIInvocationService ai;
    private NotificationRecordJpaRepository notifications;
    private FeedbackJpaRepository feedback;
    private TriageAccuracyFeedbackJpaRepository triageAccuracy;
    private AICallRecordJpaRepository aiCalls;
    private NotificationWebSocketHandler ws;
    private WorkflowService service;

    @BeforeEach
    void setUp() {
        departments = org.mockito.Mockito.mock(DepartmentJpaRepository.class);
        doctors = org.mockito.Mockito.mock(DoctorJpaRepository.class);
        patients = org.mockito.Mockito.mock(PatientJpaRepository.class);
        schedules = org.mockito.Mockito.mock(ScheduleJpaRepository.class);
        triageRecords = org.mockito.Mockito.mock(TriageRecordJpaRepository.class);
        registrations = org.mockito.Mockito.mock(RegistrationJpaRepository.class);
        notes = org.mockito.Mockito.mock(ConsultationNoteJpaRepository.class);
        medicalRecords = org.mockito.Mockito.mock(MedicalRecordJpaRepository.class);
        diagnosisSuggestions = org.mockito.Mockito.mock(DiagnosisSuggestionRecordJpaRepository.class);
        drugs = org.mockito.Mockito.mock(DrugJpaRepository.class);
        prescriptions = org.mockito.Mockito.mock(PrescriptionJpaRepository.class);
        prescriptionItems = org.mockito.Mockito.mock(PrescriptionItemJpaRepository.class);
        prescriptionReviews = org.mockito.Mockito.mock(PrescriptionReviewJpaRepository.class);
        rules = org.mockito.Mockito.mock(PrescriptionRuleDefinitionJpaRepository.class);
        ai = org.mockito.Mockito.mock(AIInvocationService.class);
        notifications = org.mockito.Mockito.mock(NotificationRecordJpaRepository.class);
        feedback = org.mockito.Mockito.mock(FeedbackJpaRepository.class);
        triageAccuracy = org.mockito.Mockito.mock(TriageAccuracyFeedbackJpaRepository.class);
        aiCalls = org.mockito.Mockito.mock(AICallRecordJpaRepository.class);
        ws = org.mockito.Mockito.mock(NotificationWebSocketHandler.class);
        service = new WorkflowService(
                departments,
                doctors,
                patients,
                schedules,
                triageRecords,
                registrations,
                notes,
                medicalRecords,
                diagnosisSuggestions,
                drugs,
                prescriptions,
                prescriptionItems,
                prescriptionReviews,
                rules,
                org.mockito.Mockito.mock(AIConfigJpaRepository.class),
                org.mockito.Mockito.mock(PromptTemplateJpaRepository.class),
                ai,
                notifications,
                feedback,
                triageAccuracy,
                aiCalls,
                ws,
                new TransactionTemplate(noopTransactionManager())
        );
    }

    @Test
    void catalogQueriesReturnOnlyActiveDepartmentsDoctorsSchedulesAndDrugs() {
        DepartmentEntity cardiology = department(1L, "cardiology", "心内科", "ACTIVE");
        DepartmentEntity inactive = department(2L, "neurology", "神经内科", "INACTIVE");
        DoctorEntity doctor = doctor(10L, "doctor01", "张医生", 1L, "ACTIVE");
        DoctorEntity inactiveDoctor = doctor(11L, "doctor02", "李医生", 1L, "INACTIVE");
        ScheduleEntity available = schedule(20L, 10L, 1L, LocalDate.now().plusDays(1), "上午", 10, 5, "ACTIVE");
        ScheduleEntity past = schedule(21L, 10L, 1L, LocalDate.now().minusDays(1), "下午", 10, 5, "ACTIVE");
        DrugEntity aspirin = drug(30L, "D001", "阿司匹林", "ASP", "ACTIVE");
        DrugEntity inactiveDrug = drug(31L, "D002", "布洛芬", "BLF", "INACTIVE");

        when(departments.findByStatusOrderByNameAsc("ACTIVE")).thenReturn(List.of(cardiology));
        when(departments.findAll()).thenReturn(List.of(cardiology, inactive));
        when(departments.findById(1L)).thenReturn(Optional.of(cardiology));
        when(doctors.findAll()).thenReturn(List.of(inactiveDoctor, doctor));
        when(doctors.findByDepartmentIdAndStatusOrderByNameAsc(1L, "ACTIVE")).thenReturn(List.of(doctor));
        when(doctors.findByIdAndStatus(10L, "ACTIVE")).thenReturn(Optional.of(doctor));
        when(schedules.findByStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc("ACTIVE", 0)).thenReturn(List.of(past, available));
        when(schedules.findByDepartmentIdAndStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc(1L, "ACTIVE", 0)).thenReturn(List.of(available));
        when(schedules.findAll()).thenReturn(List.of(past, available));
        when(drugs.findAll()).thenReturn(List.of(inactiveDrug, aspirin));
        when(drugs.findByNameContainingAndStatusOrderByNameAsc("阿司", "ACTIVE")).thenReturn(List.of(aspirin));
        when(drugs.findByPinyinCodeContainingAndStatusOrderByPinyinCodeAsc("阿司", "ACTIVE")).thenReturn(List.of());

        assertThat(service.listDepartments()).extracting("name").containsExactly("心内科");
        assertThat(service.listDoctors(null)).extracting("name").containsExactly("张医生");
        assertThat(service.listDoctors(1L)).extracting("departmentName").containsExactly("心内科");
        assertThat(service.getDoctor(10L).name()).isEqualTo("张医生");
        assertThat(service.getDepartment(1L).name()).isEqualTo("心内科");
        assertThat(service.listAvailableSchedules(null)).extracting("id").containsExactly(20L);
        assertThat(service.listAvailableSchedules(1L)).extracting("doctorName").containsExactly("张医生");
        assertThat(service.listAllSchedules(null)).extracting("id").containsExactly(21L, 20L);
        assertThat(service.listAvailableSchedulesForDoctor(10L)).extracting("id").containsExactly(20L);
        assertThat(service.searchDrugs(null)).extracting("name").containsExactly("阿司匹林");
        assertThat(service.searchDrugs("阿司")).extracting("code").containsExactly("D001");
    }

    @Test
    void triageAndRegistrationFlowCoversPatientCoreBusiness() {
        DepartmentEntity dept = department(1L, "internal-medicine", "内科", "ACTIVE");
        DoctorEntity doctor = doctor(10L, "doctor01", "张医生", 1L, "ACTIVE");
        ScheduleEntity slot = schedule(20L, 10L, 1L, LocalDate.now().plusDays(1), "上午", 10, 3, "ACTIVE");
        TriageRecordEntity savedTriage = new TriageRecordEntity();
        RegistrationEntity savedRegistration = registration(40L, 100L, 10L, 1L, 20L, "WAITING");
        savedRegistration.setTriageRecordId(50L);

        when(departments.findByCode("internal-medicine")).thenReturn(Optional.of(dept));
        when(departments.findByStatusOrderByNameAsc("ACTIVE")).thenReturn(List.of(dept));
        when(departments.findAll()).thenReturn(List.of(dept));
        when(departments.findById(1L)).thenReturn(Optional.of(dept));
        when(doctors.findByDepartmentIdAndStatusOrderByNameAsc(1L, "ACTIVE")).thenReturn(List.of(doctor));
        when(doctors.findByIdAndStatus(10L, "ACTIVE")).thenReturn(Optional.of(doctor));
        when(doctors.findById(10L)).thenReturn(Optional.of(doctor));
        when(schedules.findByDepartmentIdAndStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc(1L, "ACTIVE", 0)).thenReturn(List.of(slot));
        when(schedules.findById(20L)).thenReturn(Optional.of(slot));
        when(schedules.decrementSlotWithVersion(20L, 0)).thenReturn(1);
        when(registrations.existsByPatientIdAndScheduleIdAndStatusNot(100L, 20L, "CANCELLED")).thenReturn(false);
        when(registrations.saveAndFlush(any(RegistrationEntity.class))).thenAnswer(invocation -> {
            RegistrationEntity entity = invocation.getArgument(0);
            entity.setId(40L);
            return entity;
        });
        when(registrations.findById(40L)).thenReturn(Optional.of(savedRegistration));
        when(patients.findById(100L)).thenReturn(Optional.of(patient(100L, "王一", 35)));
        when(ai.chat(eq("TRIAGE"), eq("internal-medicine"), any(), any(), any(), eq(false), eq(null), eq(null)))
                .thenReturn(new AIModels.AIExecutionOutcome<>(
                        "recommendedDepartment: 内科\nreason: 建议内科",
                        AIModels.AIInvocationMeta.local("TRIAGE", "v1", "建议内科", false, null)
                ));
        when(aiCalls.save(any(AICallRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 60L));
        when(triageRecords.save(any(TriageRecordEntity.class))).thenAnswer(invocation -> {
            TriageRecordEntity entity = invocation.getArgument(0);
            entity.setId(50L);
            return entity;
        });
        when(triageRecords.findById(50L)).thenReturn(Optional.of(savedTriage));

        var triage = service.triage(patientActor(), new TriageRequest("  发热咳嗽  "));
        var confirmed = service.confirmConversationTriage(patientActor(),
                new ConversationTriageConfirmRequest("发热", "内科", "internal-medicine", "", "urgent"));
        var registration = service.createRegistration(patientActor(), new RegistrationCreateRequest(20L, 50L));

        assertThat(triage.recommendedDept()).isEqualTo("内科");
        assertThat(triage.recommendedDoctors()).hasSize(1);
        assertThat(confirmed.reason()).contains("urgent");
        assertThat(registration.id()).isEqualTo(40L);
        assertThat(registration.status()).isEqualTo("WAITING");
        verify(schedules).decrementSlotWithVersion(20L, 0);
    }

    @Test
    void triageUsesAiRecommendedDepartmentWhenItDiffersFromLocalRule() {
        DepartmentEntity internal = department(1L, "internal-medicine", "Internal", "ACTIVE");
        DepartmentEntity cardiology = department(2L, "cardiology", "Cardiology", "ACTIVE");
        DoctorEntity localDoctor = doctor(10L, "doc-internal", "Internal Doctor", 1L, "ACTIVE");
        DoctorEntity cardioDoctor = doctor(12L, "doc-cardio", "Cardio Doctor", 2L, "ACTIVE");
        ScheduleEntity cardioSlot = schedule(22L, 12L, 2L, LocalDate.now().plusDays(2), "AM", 8, 4, "ACTIVE");

        when(departments.findByCode("internal-medicine")).thenReturn(Optional.of(internal));
        when(departments.findByCode("cardiology")).thenReturn(Optional.of(cardiology));
        when(departments.findAll()).thenReturn(List.of(internal, cardiology));
        when(departments.findById(2L)).thenReturn(Optional.of(cardiology));
        when(doctors.findAll()).thenReturn(List.of(localDoctor, cardioDoctor));
        when(doctors.findByDepartmentIdAndStatusOrderByNameAsc(1L, "ACTIVE")).thenReturn(List.of(localDoctor));
        when(doctors.findByDepartmentIdAndStatusOrderByNameAsc(2L, "ACTIVE")).thenReturn(List.of(cardioDoctor));
        when(schedules.findByDepartmentIdAndStatusAndRemainingSlotsGreaterThanOrderByWorkDateAscPeriodAsc(2L, "ACTIVE", 0))
                .thenReturn(List.of(cardioSlot));
        when(ai.chat(eq("TRIAGE"), eq("internal-medicine"), any(), any(), any(), eq(false), eq(null), eq(null)))
                .thenReturn(outcome("TRIAGE",
                        "recommendedDepartmentCode: cardiology\nrecommendedDepartment: Cardiology\nreason: remote"));
        when(aiCalls.save(any(AICallRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 61L));
        when(triageRecords.save(any(TriageRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 51L));

        var response = service.triage(patientActor(), new TriageRequest(
                "general symptom",
                List.of(new AiContentAttachment("image", "https://example.test/image.png", null, null, "high", null))
        ));

        assertThat(response.recommendedDepartmentId()).isEqualTo(2L);
        assertThat(response.recommendedDoctors()).extracting("name").containsExactly("Cardio Doctor");
        assertThat(response.availableSchedules()).extracting("id").containsExactly(22L);
        assertThat(response.aiRecommendedDepartmentId()).isEqualTo(2L);
    }

    @Test
    void registrationCreationCoversUnavailableDuplicateOptimisticAndPersistenceConflictBranches() {
        ScheduleEntity unavailable = schedule(20L, 10L, 1L, LocalDate.now().plusDays(1), "AM", 8, 0, "ACTIVE");
        when(schedules.findById(20L)).thenReturn(Optional.of(unavailable));
        assertThatThrownBy(() -> service.createRegistration(patientActor(), new RegistrationCreateRequest(20L, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("schedule is unavailable");

        ScheduleEntity duplicate = schedule(21L, 10L, 1L, LocalDate.now().plusDays(1), "AM", 8, 1, "ACTIVE");
        when(schedules.findById(21L)).thenReturn(Optional.of(duplicate));
        when(registrations.existsByPatientIdAndScheduleIdAndStatusNot(100L, 21L, "CANCELLED")).thenReturn(true);
        assertThatThrownBy(() -> service.createRegistration(patientActor(), new RegistrationCreateRequest(21L, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("you already have an active registration for this schedule");

        ScheduleEntity changed = schedule(22L, 10L, 1L, LocalDate.now().plusDays(1), "AM", 8, 1, "ACTIVE");
        when(schedules.findById(22L)).thenReturn(Optional.of(changed));
        when(registrations.existsByPatientIdAndScheduleIdAndStatusNot(100L, 22L, "CANCELLED")).thenReturn(false);
        when(schedules.decrementSlotWithVersion(22L, 0)).thenReturn(0);
        assertThatThrownBy(() -> service.createRegistration(patientActor(), new RegistrationCreateRequest(22L, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("schedule slot changed, please retry");

        DepartmentEntity dept = department(1L, "internal-medicine", "Internal", "ACTIVE");
        DoctorEntity doctor = doctor(10L, "doc", "Doctor", 1L, "ACTIVE");
        ScheduleEntity raced = schedule(23L, 10L, 1L, LocalDate.now().plusDays(1), "AM", 8, 1, "ACTIVE");
        when(schedules.findById(23L)).thenReturn(Optional.of(raced));
        when(registrations.existsByPatientIdAndScheduleIdAndStatusNot(100L, 23L, "CANCELLED")).thenReturn(false);
        when(schedules.decrementSlotWithVersion(23L, 0)).thenReturn(1);
        when(doctors.findByIdAndStatus(10L, "ACTIVE")).thenReturn(Optional.of(doctor));
        when(departments.findById(1L)).thenReturn(Optional.of(dept));
        when(registrations.saveAndFlush(any(RegistrationEntity.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.createRegistration(patientActor(), new RegistrationCreateRequest(23L, null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("you already have an active registration for this schedule");
        verify(schedules).releaseSlotOnce(23L);
    }

    @Test
    void consultationMedicalRecordDiagnosisAndPrescriptionFlowCoversDoctorCoreBusiness() {
        DepartmentEntity dept = department(1L, "internal-medicine", "内科", "ACTIVE");
        DoctorEntity doctor = doctor(10L, "doctor01", "张医生", 1L, "ACTIVE");
        PatientEntity patient = patient(100L, "王一", 70);
        RegistrationEntity registration = registration(40L, 100L, 10L, 1L, 20L, "WAITING");
        ScheduleEntity schedule = schedule(20L, 10L, 1L, LocalDate.now(), "上午", 10, 2, "ACTIVE");
        DrugEntity aspirin = drug(30L, "D001", "阿司匹林", "ASP", "ACTIVE");

        when(registrations.findByIdAndDoctorId(40L, 10L)).thenReturn(Optional.of(registration));
        when(registrations.save(any(RegistrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(patients.findById(100L)).thenReturn(Optional.of(patient));
        when(doctors.findById(10L)).thenReturn(Optional.of(doctor));
        when(departments.findById(1L)).thenReturn(Optional.of(dept));
        when(schedules.findById(20L)).thenReturn(Optional.of(schedule));
        when(ai.chat(eq("MEDICAL_RECORD"), eq("internal-medicine"), any(), any(), any(), eq(false), eq(null), eq(null)))
                .thenReturn(outcome("MEDICAL_RECORD", "chiefComplaint: 咳嗽\npreliminaryDiagnosis: 上呼吸道感染\ntreatmentPlan: 休息"));
        when(ai.chat(eq("DIAGNOSIS"), eq("internal-medicine"), any(), any(), any(), eq(false), eq(null), eq(null)))
                .thenReturn(outcome("DIAGNOSIS", "suggestedDiagnoses: 上呼吸道感染\nsuggestedExamItems: 血常规\nfinalDiagnosisDirection: 感冒"));
        when(ai.chat(eq("PRESCRIPTION_REVIEW"), eq("internal-medicine"), any(), any(), any(), eq(false), eq(null), eq(null)))
                .thenReturn(outcome("PRESCRIPTION_REVIEW", "llmSuggestion: 可提交\nllmSummary: 低风险"));
        when(aiCalls.save(any(AICallRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 90L));
        when(medicalRecords.save(any(MedicalRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 80L));
        when(medicalRecords.findFirstByRegistrationIdOrderByVersionDesc(40L)).thenReturn(Optional.empty());

        var started = service.startConsultation(doctorActor(), 40L);
        assertThat(started.status()).isEqualTo("IN_CONSULTATION");

        var generated = service.generateMedicalRecord(doctorActor(),
                new MedicalRecordGenerateRequest(40L, "咳嗽三天。无发热。", "感冒"));
        assertThat(generated.chiefComplaint()).contains("咳嗽");

        MedicalRecordEntity storedRecord = medicalRecord(80L, 40L, 100L, 10L, "咳嗽", "感冒");
        when(medicalRecords.findFirstByRegistrationIdOrderByVersionDesc(40L)).thenReturn(Optional.of(storedRecord));
        var saved = service.saveMedicalRecord(doctorActor(), new MedicalRecordSaveRequest(
                40L, "问诊文本", "咳嗽", "咳嗽三天", "", "", "感冒", "休息", "备注", false));
        assertThat(saved.preliminaryDiagnosis()).isEqualTo("感冒");
        assertThat(registration.getStatus()).isEqualTo("MEDICAL_RECORD_SAVED");

        when(diagnosisSuggestions.save(any(DiagnosisSuggestionRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 81L));
        var suggestion = service.suggestDiagnosis(doctorActor(), new DiagnosisSuggestionRequest(40L, "咳嗽", "感冒"));
        assertThat(suggestion.suggestedExamItems()).contains("血常规");

        DiagnosisSuggestionRecordEntity suggestionEntity = diagnosisSuggestion(81L, 40L, 10L);
        when(diagnosisSuggestions.findById(81L)).thenReturn(Optional.of(suggestionEntity));
        assertThat(service.adoptDiagnosisSuggestion(doctorActor(), 81L, new DiagnosisSuggestionAdoptRequest("上感")).adoptionStatus())
                .isEqualTo("ADOPTED");
        assertThat(service.ignoreDiagnosisSuggestion(doctorActor(), 81L, new DiagnosisSuggestionIgnoreRequest("临床不符")).adoptionStatus())
                .isEqualTo("IGNORED");

        PrescriptionItemRequest item = new PrescriptionItemRequest(30L, new BigDecimal("1.00"), "bid", "3天", 1, "饭后");
        when(drugs.findByIdAndStatus(30L, "ACTIVE")).thenReturn(Optional.of(aspirin));
        when(rules.findByStatusOrderByRuleCodeAsc("ACTIVE")).thenReturn(List.of());
        when(prescriptionReviews.save(any(PrescriptionReviewEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 82L));
        when(notifications.findFirstByRecipientIdAndRecipientRoleAndAlertTypeAndBusinessRecordIdAndReadFalseOrderByCreatedAtDesc(
                any(), any(), any(), any())).thenReturn(Optional.empty());

        var review = service.reviewPrescription(doctorActor(), new PrescriptionReviewRequest(40L, List.of(item)));
        assertThat(review.riskLevel()).isEqualTo("LOW");
        assertThat(registration.getStatus()).isEqualTo("PRESCRIPTION_REVIEWED");

        PrescriptionReviewEntity reviewEntity = reviewEntity(82L, 40L, 100L, 10L, review.prescriptionSnapshotHash(), review.reviewContextHash());
        when(prescriptionReviews.findByIdAndBindStatus(82L, "UNBOUND")).thenReturn(Optional.of(reviewEntity));
        when(prescriptions.save(any(PrescriptionEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 83L));
        when(prescriptionItems.save(any(PrescriptionItemEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 84L));
        when(prescriptionItems.findByPrescriptionIdOrderByCreatedAtAsc(83L)).thenReturn(List.of());

        var prescription = service.submitPrescription(doctorActor(), new PrescriptionSubmitRequest(40L, 82L, List.of(item), "确认"));
        assertThat(prescription.id()).isEqualTo(83L);
        assertThat(registration.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void saveMedicalRecordCoversStateGuardsAndNewRecordCreation() {
        for (String status : List.of("WAITING", "COMPLETED", "CANCELLED", "ARCHIVED")) {
            Long registrationId = 500L + status.length();
            when(registrations.findByIdAndDoctorId(registrationId, 10L))
                    .thenReturn(Optional.of(registration(registrationId, 100L, 10L, 1L, 20L, status)));

            assertThatThrownBy(() -> service.saveMedicalRecord(doctorActor(), new MedicalRecordSaveRequest(
                    registrationId, "conversation", "chief", "illness", "", "", "diagnosis", "plan", "", false)))
                    .isInstanceOf(ApiException.class)
                    .satisfies(error -> assertThat(((ApiException) error).getCode()).isEqualTo(409));
        }

        RegistrationEntity active = registration(60L, 100L, 10L, 1L, 20L, "IN_CONSULTATION");
        when(registrations.findByIdAndDoctorId(60L, 10L)).thenReturn(Optional.of(active));
        when(registrations.save(any(RegistrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(medicalRecords.findFirstByRegistrationIdOrderByVersionDesc(60L)).thenReturn(Optional.empty());
        when(medicalRecords.save(any(MedicalRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 600L));

        var saved = service.saveMedicalRecord(doctorActor(), new MedicalRecordSaveRequest(
                60L, "conversation", "", "", "", "", "", "", "note", true));

        assertThat(saved.id()).isEqualTo(600L);
        assertThat(saved.version()).isZero();
        assertThat(saved.aiGenerated()).isTrue();
        assertThat(active.getStatus()).isEqualTo("MEDICAL_RECORD_SAVED");
    }

    @Test
    void prescriptionReviewCoversHighRiskLocalRulesAndDoctorNotification() {
        RegistrationEntity registration = registration(70L, 100L, 10L, 1L, 20L, "MEDICAL_RECORD_SAVED");
        PatientEntity patient = patient(100L, "Risk Patient", 70);
        patient.setAllergyHistory("penicillin");
        patient.setMedicalHistory("ulcer");
        DrugEntity aspirin = drug(30L, "D001", "Aspirin", "ASP", "ACTIVE");
        aspirin.setContraindications("penicillin ulcer");
        aspirin.setPrecautions("70");
        aspirin.setInteractionSummary("Warfarin");
        DrugEntity warfarin = drug(31L, "D002", "Warfarin", "WAR", "ACTIVE");
        warfarin.setInteractionSummary("Aspirin");
        MedicalRecordEntity record = medicalRecord(700L, 70L, 100L, 10L, "pain", "ulcer");
        PrescriptionRuleDefinitionEntity customRule = prescriptionRule(900L, "CUSTOM_QTY", "CUSTOM_RULE", "Aspirin", "ulcer", "70", "quantity>10", "MEDIUM");

        when(registrations.findByIdAndDoctorId(70L, 10L)).thenReturn(Optional.of(registration));
        when(patients.findById(100L)).thenReturn(Optional.of(patient));
        when(medicalRecords.findFirstByRegistrationIdOrderByVersionDesc(70L)).thenReturn(Optional.of(record));
        when(drugs.findByIdAndStatus(30L, "ACTIVE")).thenReturn(Optional.of(aspirin));
        when(drugs.findByIdAndStatus(31L, "ACTIVE")).thenReturn(Optional.of(warfarin));
        when(rules.findByStatusOrderByRuleCodeAsc("ACTIVE")).thenReturn(List.of(customRule));
        when(ai.chat(eq("PRESCRIPTION_REVIEW"), eq(null), any(), any(), any(), eq(false), eq(null), eq(null)))
                .thenReturn(outcome("PRESCRIPTION_REVIEW", "llmSuggestion: review\nllmSummary: high risk"));
        when(aiCalls.save(any(AICallRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 701L));
        when(prescriptionReviews.save(any(PrescriptionReviewEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 702L));
        when(notifications.findFirstByRecipientIdAndRecipientRoleAndAlertTypeAndBusinessRecordIdAndReadFalseOrderByCreatedAtDesc(
                any(), any(), any(), any())).thenReturn(Optional.empty());
        when(notifications.save(any(NotificationRecordEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 703L));
        when(registrations.save(any(RegistrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var item = new PrescriptionItemRequest(30L, new BigDecimal("3.00"), "qid", "14 days", 40, "after meal");
        var duplicate = new PrescriptionItemRequest(30L, new BigDecimal("1.00"), "bid", "3 days", 1, "after meal");
        var interacting = new PrescriptionItemRequest(31L, new BigDecimal("1.00"), "qd", "3 days", 1, "after meal");

        var review = service.reviewPrescription(doctorActor(), new PrescriptionReviewRequest(70L, List.of(item, duplicate, interacting)));

        assertThat(review.riskLevel()).isEqualTo("HIGH");
        assertThat(review.ruleEngineStatus()).isEqualTo("SUCCESS");
        assertThat(review.ruleHits()).extracting("ruleType")
                .contains("DUPLICATE_DRUG", "ALLERGY_CONTRAINDICATION", "DISEASE_CONTRAINDICATION",
                        "SPECIAL_POPULATION", "COURSE_LIMIT", "DOSAGE_LIMIT", "FREQUENCY_LIMIT",
                        "DRUG_INTERACTION", "CUSTOM_RULE");
        assertThat(registration.getStatus()).isEqualTo("PRESCRIPTION_REVIEWED");
        verify(ws).publish(any());
    }

    @Test
    void submitPrescriptionRejectsMismatchedReviewAndChangedPrescriptionHash() {
        RegistrationEntity registration = registration(80L, 100L, 10L, 1L, 20L, "PRESCRIPTION_REVIEWED");
        PrescriptionItemRequest item = new PrescriptionItemRequest(30L, new BigDecimal("1.00"), "bid", "3 days", 1, "after meal");

        when(registrations.findByIdAndDoctorId(80L, 10L)).thenReturn(Optional.of(registration));
        PrescriptionReviewEntity otherRegistrationReview = reviewEntity(81L, 999L, 100L, 10L, "hash", "ctx");
        when(prescriptionReviews.findByIdAndBindStatus(81L, "UNBOUND")).thenReturn(Optional.of(otherRegistrationReview));

        assertThatThrownBy(() -> service.submitPrescription(doctorActor(), new PrescriptionSubmitRequest(80L, 81L, List.of(item), "")))
                .isInstanceOf(ApiException.class)
                .hasMessage("review does not belong to this registration");

        PrescriptionReviewEntity staleReview = reviewEntity(82L, 80L, 100L, 10L, "old-hash", "old-context");
        when(prescriptionReviews.findByIdAndBindStatus(82L, "UNBOUND")).thenReturn(Optional.of(staleReview));
        when(medicalRecords.findFirstByRegistrationIdOrderByVersionDesc(80L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitPrescription(doctorActor(), new PrescriptionSubmitRequest(80L, 82L, List.of(item), "")))
                .isInstanceOf(ApiException.class)
                .hasMessage("prescription changed after review, please review again");
    }

    @Test
    void listsNotificationsFeedbackAndDashboardMetricsCoverReportingCore() {
        RegistrationEntity completed = registration(40L, 100L, 10L, 1L, 20L, "COMPLETED");
        completed.setRegistrationTime(LocalDate.now().atTime(9, 0));
        completed.setCompletedTime(LocalDateTime.now());
        FeedbackEntity existingFeedback = feedback(70L, 100L, 40L, 5, true);
        NotificationRecordEntity notice = notification(71L, 10L, "DOCTOR", "HIGH_RISK_PRESCRIPTION", false);
        PrescriptionReviewEntity review = reviewEntity(82L, 40L, 100L, 10L, "h1", "h2");
        review.setRiskLevel("HIGH");
        review.setContextMissingItems("缺少过敏史");
        AICallRecordEntity call = aiCall("DIAGNOSIS", 40L, 2L, "DOCTOR", false, 20L);

        when(registrations.findByIdAndPatientId(40L, 100L)).thenReturn(Optional.of(completed));
        when(registrations.findByPatientIdOrderByRegistrationTimeDesc(100L)).thenReturn(List.of(completed));
        when(registrations.findByDoctorIdOrderByRegistrationTimeDesc(10L)).thenReturn(List.of(completed));
        when(registrations.findAll()).thenReturn(List.of(completed));
        when(feedback.findByRegistrationId(40L)).thenReturn(Optional.of(existingFeedback));
        when(feedback.findByPatientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(existingFeedback));
        when(feedback.findAll()).thenReturn(List.of(existingFeedback));
        when(feedback.count()).thenReturn(1L);
        when(feedback.countByTriageAccurate(true)).thenReturn(1L);
        when(feedback.countByTriageAccurate(false)).thenReturn(0L);
        when(notifications.findByRecipientIdAndRecipientRoleAndReadFalseOrderByCreatedAtDesc(10L, "DOCTOR")).thenReturn(List.of(notice));
        when(notifications.findById(71L)).thenReturn(Optional.of(notice));
        when(notifications.save(any(NotificationRecordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(prescriptionReviews.findAll()).thenReturn(List.of(review));
        when(prescriptionReviews.findByDoctorIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(review));
        when(aiCalls.findAll()).thenReturn(List.of(call));
        when(prescriptions.findAll()).thenReturn(List.of(prescription(83L, 40L, 100L, 10L, 82L, "HIGH")));
        when(medicalRecords.findAll()).thenReturn(List.of(medicalRecord(80L, 40L, 100L, 10L, "咳嗽", "感冒")));
        when(triageRecords.findByPatientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(triageRecord(50L, 100L, "咳嗽")));
        when(triageRecords.findByCallStatusOrderByCreatedAtDesc("COMPLETED")).thenReturn(List.of(triageRecord(51L, 101L, "胸痛")));

        assertThat(service.createFeedback(patientActor(), new FeedbackCreateRequest(40L, 5, true, "满意")).id()).isEqualTo(70L);
        assertThat(service.listPatientFeedback(patientActor())).hasSize(1);
        assertThat(service.listPatientRegistrations(patientActor())).hasSize(1);
        assertThat(service.listDoctorQueue(doctorActor())).isEmpty();
        assertThat(service.listUnreadNotifications(doctorActor())).hasSize(1);
        assertThat(service.markNotificationRead(doctorActor(), 71L).read()).isTrue();
        assertThat(service.overview(doctorActor()).highRiskReviews()).isEqualTo(1);
        assertThat(service.dashboardTrends(doctorActor(), LocalDate.now(), LocalDate.now())).hasSize(1);
        assertThat(service.dashboardAiUsage(doctorActor(), null).totalCalls()).isEqualTo(1);
        assertThat(service.dashboardPrescriptionReviewRate(doctorActor()).highRiskReviews()).isEqualTo(1);
        assertThat(service.dashboardRiskDistribution(doctorActor()).buckets()).isNotEmpty();
        assertThat(service.dashboardTriageAccuracy(doctorActor()).accuracyRate()).isEqualTo(100D);
        assertThat(service.listTriageRecords(patientActor())).hasSize(1);
        assertThat(service.listTriageRecords(adminActor())).hasSize(1);
        assertThatThrownBy(() -> service.overview(patientActor()))
                .isInstanceOf(ApiException.class)
                .hasMessage("dashboard permission required");
    }

    @Test
    void feedbackCreationAndNotificationPermissionsCoverPatientDoctorBoundaries() {
        RegistrationEntity completed = registration(90L, 100L, 10L, 1L, 20L, "COMPLETED");
        completed.setTriageRecordId(50L);
        FeedbackEntity savedFeedback = feedback(91L, 100L, 90L, 4, false);
        NotificationRecordEntity patientNotice = notification(92L, 100L, "PATIENT", "FOLLOW_UP", false);
        NotificationRecordEntity otherDoctorNotice = notification(93L, 999L, "DOCTOR", "HIGH_RISK_PRESCRIPTION", false);

        when(registrations.findByIdAndPatientId(90L, 100L)).thenReturn(Optional.of(completed));
        when(feedback.findByRegistrationId(90L)).thenReturn(Optional.empty());
        when(feedback.save(any(FeedbackEntity.class))).thenReturn(savedFeedback);
        when(triageAccuracy.findByFeedbackId(91L)).thenReturn(Optional.empty());
        when(triageRecords.findById(50L)).thenReturn(Optional.of(triageRecord(50L, 100L, "symptom")));

        var response = service.createFeedback(patientActor(), new FeedbackCreateRequest(90L, 4, false, "not accurate"));

        assertThat(response.id()).isEqualTo(91L);
        verify(triageAccuracy).save(any(TriageAccuracyFeedbackEntity.class));

        when(notifications.findById(92L)).thenReturn(Optional.of(patientNotice));
        when(notifications.findById(93L)).thenReturn(Optional.of(otherDoctorNotice));
        when(notifications.save(any(NotificationRecordEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.markNotificationRead(patientActor(), 92L).read()).isTrue();
        assertThatThrownBy(() -> service.markNotificationRead(doctorActor(), 93L))
                .isInstanceOf(ApiException.class)
                .hasMessage("notification permission required");
    }

    @Test
    void prescriptionReviewLookupFallsBackToLatestReviewAndReportsMissingReview() {
        PrescriptionEntity prescription = prescription(83L, 41L, 100L, 10L, null, "LOW");
        PrescriptionReviewEntity latest = reviewEntity(82L, 41L, 100L, 10L, "hash", "ctx");
        latest.setPrescriptionId(83L);

        when(prescriptions.findById(83L)).thenReturn(Optional.of(prescription));
        when(prescriptionReviews.findByPrescriptionIdOrderByCreatedAtDesc(83L)).thenReturn(List.of(latest));
        when(prescriptionItems.findByPrescriptionIdOrderByCreatedAtAsc(83L)).thenReturn(List.of());

        assertThat(service.getPrescriptionReview(patientActor(), 83L).reviewId()).isEqualTo(82L);

        when(prescriptionReviews.findByPrescriptionIdOrderByCreatedAtDesc(83L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.getPrescriptionReview(patientActor(), 83L))
                .isInstanceOf(ApiException.class)
                .hasMessage("prescription review not found");
    }

    @Test
    void cancellationWorkspaceCompletionRecordsPrescriptionsAndDashboardBoundariesCoverReadCore() {
        RegistrationEntity waiting = registration(40L, 100L, 10L, 1L, 20L, "WAITING");
        RegistrationEntity savedRecordRegistration = registration(41L, 100L, 10L, 1L, 20L, "MEDICAL_RECORD_SAVED");
        MedicalRecordEntity record = medicalRecord(80L, 41L, 100L, 10L, "cough", "cold");
        PrescriptionReviewEntity review = reviewEntity(82L, 41L, 100L, 10L, "hash", "ctx");
        review.setPrescriptionId(83L);
        review.setBindStatus("BOUND");
        PrescriptionEntity prescription = prescription(83L, 41L, 100L, 10L, 82L, "LOW");
        PrescriptionItemEntity item = prescriptionItem(84L, 83L, 30L);
        DrugEntity aspirin = drug(30L, "D001", "Aspirin", "ASP", "ACTIVE");
        NotificationRecordEntity adminNotice = notification(72L, 3L, "ADMIN", "SYSTEM", false);
        AICallRecordEntity diagnosisCall = aiCall("DIAGNOSIS", 41L, 2L, "DOCTOR", false, 30L);
        AICallRecordEntity triageCall = aiCall("TRIAGE", 50L, 1L, "PATIENT", true, 50L);

        when(registrations.findByIdAndPatientId(40L, 100L)).thenReturn(Optional.of(waiting));
        when(registrations.cancelWaitingRegistrationOnce(eq(40L), eq(100L), eq("WAITING"), eq("CANCELLED"), eq("patient cancelled"), any()))
                .thenReturn(1);
        RegistrationEntity cancelled = registration(40L, 100L, 10L, 1L, 20L, "CANCELLED");
        cancelled.setSlotReleased(true);
        when(registrations.findById(40L)).thenReturn(Optional.of(cancelled));
        assertThat(service.cancelRegistration(patientActor(), 40L, new RegistrationCancelRequest("")).status()).isEqualTo("CANCELLED");
        verify(schedules).releaseSlotOnce(20L);

        waiting.setStatus("COMPLETED");
        assertThatThrownBy(() -> service.cancelRegistration(patientActor(), 40L, new RegistrationCancelRequest("late")))
                .isInstanceOf(ApiException.class)
                .hasMessage("only waiting registrations can be cancelled");

        when(registrations.findByIdAndDoctorId(41L, 10L)).thenReturn(Optional.of(savedRecordRegistration));
        when(medicalRecords.findFirstByRegistrationIdOrderByVersionDesc(41L)).thenReturn(Optional.of(record));
        when(prescriptions.findByRegistrationIdOrderByCreatedAtDesc(41L)).thenReturn(List.of(prescription));
        when(prescriptionReviews.findByRegistrationIdOrderByCreatedAtDesc(41L)).thenReturn(List.of(review));
        when(prescriptionReviews.findById(82L)).thenReturn(Optional.of(review));
        when(prescriptionItems.findByPrescriptionIdOrderByCreatedAtAsc(83L)).thenReturn(List.of(item));
        when(drugs.findById(30L)).thenReturn(Optional.of(aspirin));
        when(patients.findById(100L)).thenReturn(Optional.of(patient(100L, "Alice", 66)));
        when(doctors.findById(10L)).thenReturn(Optional.of(doctor(10L, "doctor", "Doctor", 1L, "ACTIVE")));
        when(departments.findById(1L)).thenReturn(Optional.of(department(1L, "internal", "Internal", "ACTIVE")));
        when(registrations.save(any(RegistrationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var workspace = service.getConsultationWorkspace(doctorActor(), 41L);
        assertThat(workspace.latestMedicalRecord().id()).isEqualTo(80L);
        assertThat(workspace.latestPrescription().id()).isEqualTo(83L);
        assertThat(workspace.recentReviews()).hasSize(1);
        assertThat(workspace.nextActions()).isNotEmpty();

        assertThat(service.completeConsultation(doctorActor(), 41L).status()).isEqualTo("COMPLETED");
        savedRecordRegistration.setStatus("COMPLETED");
        assertThat(service.completeConsultation(doctorActor(), 41L).status()).isEqualTo("COMPLETED");
        savedRecordRegistration.setStatus("WAITING");
        assertThatThrownBy(() -> service.completeConsultation(doctorActor(), 41L))
                .isInstanceOf(ApiException.class)
                .hasMessage("consultation can only be completed after medical record is saved");

        when(medicalRecords.findByPatientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(record));
        when(medicalRecords.findByDoctorIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(record));
        when(medicalRecords.findById(80L)).thenReturn(Optional.of(record));
        assertThat(service.listPatientMedicalRecords(patientActor())).hasSize(1);
        assertThat(service.listMedicalRecordsForPatient(doctorActor(), 100L)).hasSize(1);
        assertThat(service.listDoctorMedicalRecords(doctorActor())).hasSize(1);
        assertThat(service.searchDoctorMedicalRecords(doctorActor(), "cold")).hasSize(1);
        assertThat(service.getMedicalRecord(adminActor(), 80L).id()).isEqualTo(80L);
        assertThatThrownBy(() -> service.getMedicalRecord(patientActor(), 80L + 1))
                .isInstanceOf(ApiException.class)
                .hasMessage("medical record not found");

        when(prescriptions.findByPatientIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(prescription));
        when(prescriptions.findByDoctorIdOrderByCreatedAtDesc(10L)).thenReturn(List.of(prescription));
        when(prescriptions.findById(83L)).thenReturn(Optional.of(prescription));
        when(prescriptionReviews.findByPrescriptionIdOrderByCreatedAtDesc(83L)).thenReturn(List.of(review));
        assertThat(service.listPatientPrescriptions(patientActor())).hasSize(1);
        assertThat(service.listDoctorPrescriptions(doctorActor())).hasSize(1);
        assertThat(service.getPrescription(patientActor(), 83L).id()).isEqualTo(83L);
        assertThat(service.getPrescriptionReview(doctorActor(), 83L).reviewId()).isEqualTo(82L);
        assertThatThrownBy(() -> service.getPrescription(new ActorContext(9L, ActorRole.PATIENT, 999L, null, "other", "Other"), 83L))
                .isInstanceOf(ApiException.class)
                .hasMessage("prescription permission required");

        when(notifications.findByReadFalseOrderByCreatedAtDesc()).thenReturn(List.of(adminNotice));
        assertThat(service.listUnreadNotifications(adminActor())).hasSize(1);
        assertThatThrownBy(() -> service.listUnreadNotifications(patientActor()))
                .isInstanceOf(ApiException.class)
                .hasMessage("notification permission required");

        RegistrationEntity adminVisible = registration(42L, 101L, 11L, 1L, 21L, "COMPLETED");
        adminVisible.setRegistrationTime(LocalDate.now().atTime(10, 0));
        when(registrations.findAll()).thenReturn(List.of(adminVisible));
        when(prescriptionReviews.findAll()).thenReturn(List.of(review));
        when(aiCalls.findAll()).thenReturn(List.of(diagnosisCall, triageCall));
        assertThat(service.dashboardAiUsage(adminActor(), "DIAGNOSIS").totalCalls()).isEqualTo(1);
        assertThatThrownBy(() -> service.dashboardTrends(adminActor(), LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(ApiException.class)
                .hasMessage("startDate must not be after endDate");
        assertThatThrownBy(() -> service.dashboardTrends(adminActor(), LocalDate.now().minusDays(61), LocalDate.now()))
                .isInstanceOf(ApiException.class)
                .hasMessage("date range cannot exceed 60 days");
    }

    private ActorContext patientActor() {
        return new ActorContext(1L, ActorRole.PATIENT, 100L, null, "patient", "王一");
    }

    private ActorContext doctorActor() {
        return new ActorContext(2L, ActorRole.DOCTOR, null, 10L, "doctor", "张医生");
    }

    private ActorContext adminActor() {
        return new ActorContext(3L, ActorRole.ADMIN, null, null, "admin", "管理员");
    }

    private PlatformTransactionManager noopTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }

    private AIModels.AIExecutionOutcome<String> outcome(String taskType, String text) {
        return new AIModels.AIExecutionOutcome<>(text, AIModels.AIInvocationMeta.local(taskType, "v1", text, false, null));
    }

    private DepartmentEntity department(Long id, String code, String name, String status) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setType("门诊");
        entity.setStatus(status);
        return entity;
    }

    private DoctorEntity doctor(Long id, String username, String name, Long departmentId, String status) {
        DoctorEntity entity = new DoctorEntity();
        entity.setId(id);
        entity.setUsername(username);
        entity.setPasswordHash("{noop}pw");
        entity.setName(name);
        entity.setDepartmentId(departmentId);
        entity.setTitle("主任");
        entity.setSpecialty("内科");
        entity.setIntroduction("介绍");
        entity.setStatus(status);
        return entity;
    }

    private PatientEntity patient(Long id, String name, int age) {
        PatientEntity entity = new PatientEntity();
        entity.setId(id);
        entity.setUsername("patient" + id);
        entity.setPasswordHash("{noop}pw");
        entity.setName(name);
        entity.setAge(age);
        entity.setGender("女");
        entity.setAllergyHistory("无");
        entity.setMedicalHistory("高血压");
        entity.setRemark("");
        entity.setStatus("ACTIVE");
        return entity;
    }

    private ScheduleEntity schedule(Long id, Long doctorId, Long departmentId, LocalDate date, String period,
                                    int total, int remaining, String status) {
        ScheduleEntity entity = new ScheduleEntity();
        entity.setId(id);
        entity.setDoctorId(doctorId);
        entity.setDepartmentId(departmentId);
        entity.setWorkDate(date);
        entity.setPeriod(period);
        entity.setTotalSlots(total);
        entity.setRemainingSlots(remaining);
        entity.setVisitLevel("普通");
        entity.setStatus(status);
        entity.setVersion(0);
        return entity;
    }

    private RegistrationEntity registration(Long id, Long patientId, Long doctorId, Long departmentId, Long scheduleId, String status) {
        RegistrationEntity entity = new RegistrationEntity();
        entity.setId(id);
        entity.setPatientId(patientId);
        entity.setDoctorId(doctorId);
        entity.setDepartmentId(departmentId);
        entity.setScheduleId(scheduleId);
        entity.setRegistrationTime(LocalDateTime.now());
        entity.setDepartmentSnapshot("内科");
        entity.setDoctorSnapshot("张医生");
        entity.setVisitLevelSnapshot("普通");
        entity.setStatus(status);
        entity.setSlotReleased(false);
        entity.setVersion(0);
        return entity;
    }

    private DrugEntity drug(Long id, String code, String name, String pinyin, String status) {
        DrugEntity entity = new DrugEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setPinyinCode(pinyin);
        entity.setSpecification("100mg");
        entity.setDosageForm("片剂");
        entity.setPackageUnit("盒");
        entity.setManufacturer("厂家");
        entity.setUnitPrice(new BigDecimal("10.00"));
        entity.setDefaultUsage("口服");
        entity.setContraindications("");
        entity.setPrecautions("");
        entity.setIndications("感冒");
        entity.setInteractionSummary("");
        entity.setStatus(status);
        return entity;
    }

    private MedicalRecordEntity medicalRecord(Long id, Long registrationId, Long patientId, Long doctorId, String chief, String diagnosis) {
        MedicalRecordEntity entity = new MedicalRecordEntity();
        entity.setId(id);
        entity.setRegistrationId(registrationId);
        entity.setPatientId(patientId);
        entity.setDoctorId(doctorId);
        entity.setChiefComplaint(chief);
        entity.setPresentIllness(chief + "三天");
        entity.setPastHistory("高血压");
        entity.setPhysicalExam("体温正常");
        entity.setPreliminaryDiagnosis(diagnosis);
        entity.setTreatmentPlan("休息");
        entity.setConversationText("问诊");
        entity.setAiGenerated(false);
        entity.setVersion(0);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private DiagnosisSuggestionRecordEntity diagnosisSuggestion(Long id, Long registrationId, Long doctorId) {
        DiagnosisSuggestionRecordEntity entity = new DiagnosisSuggestionRecordEntity();
        entity.setId(id);
        entity.setRegistrationId(registrationId);
        entity.setPatientId(100L);
        entity.setDoctorId(doctorId);
        entity.setSuggestedDiagnoses("上感");
        entity.setSuggestedExamItems("血常规");
        entity.setAdoptionStatus("SUGGESTED");
        entity.setFinalDiagnosisDirection("上感");
        return entity;
    }

    private PrescriptionReviewEntity reviewEntity(Long id, Long registrationId, Long patientId, Long doctorId, String itemHash, String contextHash) {
        PrescriptionReviewEntity entity = new PrescriptionReviewEntity();
        entity.setId(id);
        entity.setRegistrationId(registrationId);
        entity.setPatientId(patientId);
        entity.setDoctorId(doctorId);
        entity.setRiskLevel("LOW");
        entity.setLocalRuleHits("[LOW] ok");
        entity.setRuleEngineStatus("SUCCESS");
        entity.setContextMissingItems("");
        entity.setLlmSuggestion("可提交");
        entity.setLlmSummary("低风险");
        entity.setLlmCallStatus("LOCAL_RULE");
        entity.setPrescriptionSnapshotHash(itemHash);
        entity.setReviewContextHash(contextHash);
        entity.setBindStatus("UNBOUND");
        entity.setVersion(0);
        return entity;
    }

    private PrescriptionEntity prescription(Long id, Long registrationId, Long patientId, Long doctorId, Long reviewId, String risk) {
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(id);
        entity.setRegistrationId(registrationId);
        entity.setPatientId(patientId);
        entity.setDoctorId(doctorId);
        entity.setReviewId(reviewId);
        entity.setRiskLevel(risk);
        entity.setStatus("SUBMITTED");
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private PrescriptionItemEntity prescriptionItem(Long id, Long prescriptionId, Long drugId) {
        PrescriptionItemEntity entity = new PrescriptionItemEntity();
        entity.setId(id);
        entity.setPrescriptionId(prescriptionId);
        entity.setDrugId(drugId);
        entity.setDosage(new BigDecimal("1.00"));
        entity.setFrequency("bid");
        entity.setDuration("3 days");
        entity.setQuantity(1);
        entity.setUsageInstruction("after meal");
        return entity;
    }

    private PrescriptionRuleDefinitionEntity prescriptionRule(Long id,
                                                              String code,
                                                              String type,
                                                              String drugs,
                                                              String diseases,
                                                              String populations,
                                                              String expression,
                                                              String risk) {
        PrescriptionRuleDefinitionEntity entity = new PrescriptionRuleDefinitionEntity();
        entity.setId(id);
        entity.setRuleCode(code);
        entity.setRuleType(type);
        entity.setApplicableDrugs(drugs);
        entity.setApplicableDiseases(diseases);
        entity.setApplicablePopulations(populations);
        entity.setConditionExpression(expression);
        entity.setRiskLevel(risk);
        entity.setAlertMessage("custom alert");
        entity.setSuggestion("custom suggestion");
        entity.setBasis("custom basis");
        entity.setSeeded(false);
        entity.setVersion(0);
        entity.setValidationStatus("VALID");
        entity.setStatus("ACTIVE");
        return entity;
    }

    private FeedbackEntity feedback(Long id, Long patientId, Long registrationId, int rating, boolean accurate) {
        FeedbackEntity entity = new FeedbackEntity();
        entity.setId(id);
        entity.setPatientId(patientId);
        entity.setRegistrationId(registrationId);
        entity.setRating(rating);
        entity.setTriageAccurate(accurate);
        entity.setComment("满意");
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private NotificationRecordEntity notification(Long id, Long recipientId, String role, String type, boolean read) {
        NotificationRecordEntity entity = new NotificationRecordEntity();
        entity.setId(id);
        entity.setRecipientId(recipientId);
        entity.setRecipientRole(role);
        entity.setAlertType(type);
        entity.setStatisticsBucket("HIGH");
        entity.setDisplayLevel("HIGH");
        entity.setBusinessRecordId(40L);
        entity.setPatientSummary("患者");
        entity.setRiskSummary("高风险");
        entity.setRead(read);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private TriageRecordEntity triageRecord(Long id, Long patientId, String complaint) {
        TriageRecordEntity entity = new TriageRecordEntity();
        entity.setId(id);
        entity.setPatientId(patientId);
        entity.setChiefComplaint(complaint);
        entity.setRecommendedDept("内科");
        entity.setRecommendedDoctors("张医生");
        entity.setAiResponseRaw("建议内科");
        entity.setCallStatus("COMPLETED");
        entity.setRecommendationSource("LOCAL_RULE");
        return entity;
    }

    private AICallRecordEntity aiCall(String taskType, Long businessId, Long operatorId, String role, boolean degraded, Long duration) {
        AICallRecordEntity entity = new AICallRecordEntity();
        entity.setId(91L);
        entity.setTaskType(taskType);
        entity.setBusinessRecordId(businessId);
        entity.setOperatorId(operatorId);
        entity.setOperatorRole(role);
        entity.setProvider("LOCAL_RULE");
        entity.setModelName("local");
        entity.setCallStatus("COMPLETED");
        entity.setDurationMs(duration);
        entity.setDegraded(degraded);
        entity.setRetryCount(0);
        entity.setCreatedAt(Instant.now());
        return entity;
    }

    private <T extends com.cloudbrain.entity.BaseAuditableEntity> T withId(T entity, Long id) {
        if (entity.getId() == null) {
            entity.setId(id);
        }
        return entity;
    }
}
