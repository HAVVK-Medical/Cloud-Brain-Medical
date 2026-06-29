package com.cloudbrain.controller;

import com.cloudbrain.application.workflow.WorkflowService;
import com.cloudbrain.application.workflow.AiStreamSessionService;
import com.cloudbrain.common.Result;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiStreamSessionCreateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiStreamSessionCreateResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiUsageStats;
import com.cloudbrain.dto.workflow.WorkflowDtos.AiCallRecordSummary;
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
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordGenerateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordSaveRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.MedicalRecordSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionReviewRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionReviewResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionReviewRate;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionSubmitRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.PrescriptionSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationCancelRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationCreateRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.RegistrationSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.RiskDistribution;
import com.cloudbrain.dto.workflow.WorkflowDtos.ScheduleOption;
import com.cloudbrain.dto.workflow.WorkflowDtos.TriageAccuracyStats;
import com.cloudbrain.dto.workflow.WorkflowDtos.TriageRequest;
import com.cloudbrain.dto.workflow.WorkflowDtos.TriageResponse;
import com.cloudbrain.dto.workflow.WorkflowDtos.AuditLogSummary;
import com.cloudbrain.dto.workflow.WorkflowDtos.NotificationRecordSummary;
import com.cloudbrain.repository.AICallRecordJpaRepository;
import com.cloudbrain.repository.AuditLogJpaRepository;
import com.cloudbrain.security.ActorContextResolver;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class WorkflowController {

    private final WorkflowService workflowService;
    private final AiStreamSessionService aiStreamSessionService;
    private final AICallRecordJpaRepository aiCallRecordRepository;
    private final AuditLogJpaRepository auditLogRepository;

    public WorkflowController(WorkflowService workflowService,
                              AiStreamSessionService aiStreamSessionService,
                              AICallRecordJpaRepository aiCallRecordRepository,
                              AuditLogJpaRepository auditLogRepository) {
        this.workflowService = workflowService;
        this.aiStreamSessionService = aiStreamSessionService;
        this.aiCallRecordRepository = aiCallRecordRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/departments")
    public Result<List<DepartmentOption>> listDepartments() {
        return Result.success(workflowService.listDepartments());
    }

    @GetMapping("/departments/{id}")
    public Result<DepartmentOption> getDepartment(@PathVariable Long id) {
        return Result.success(workflowService.getDepartment(id));
    }

    @GetMapping("/doctors")
    public Result<List<DoctorOption>> listDoctors(@RequestParam(required = false) Long departmentId) {
        return Result.success(workflowService.listDoctors(departmentId));
    }

    @GetMapping("/doctors/{id}")
    public Result<DoctorOption> getDoctor(@PathVariable Long id) {
        return Result.success(workflowService.getDoctor(id));
    }

    @GetMapping("/schedules/available")
    public Result<List<ScheduleOption>> listSchedules(@RequestParam(required = false) Long departmentId) {
        return Result.success(workflowService.listAvailableSchedules(departmentId));
    }

    @GetMapping("/schedules/doctor/{doctorId}/available")
    public Result<List<ScheduleOption>> listDoctorSchedules(@PathVariable Long doctorId) {
        return Result.success(workflowService.listAvailableSchedulesForDoctor(doctorId));
    }

    @GetMapping("/drugs/search")
    public Result<List<DrugOption>> searchDrugs(@RequestParam(required = false) String keyword) {
        return Result.success(workflowService.searchDrugs(keyword));
    }

    @PostMapping("/triage/consult")
    public Result<TriageResponse> triage(@Valid @RequestBody TriageRequest request) {
        return Result.success(workflowService.triage(ActorContextResolver.requireCurrent(), request));
    }

    @PostMapping("/triage/conversation/confirm")
    public Result<TriageResponse> confirmConversationTriage(@Valid @RequestBody ConversationTriageConfirmRequest request) {
        return Result.success(workflowService.confirmConversationTriage(ActorContextResolver.requireCurrent(), request));
    }

    @GetMapping("/triage/history")
    public Result<List<TriageResponse>> triageHistory() {
        return Result.success(workflowService.listTriageRecords(ActorContextResolver.requireCurrent()));
    }

    @PostMapping("/registration/create")
    public Result<RegistrationSummary> createRegistration(@Valid @RequestBody RegistrationCreateRequest request) {
        return Result.success(workflowService.createRegistration(ActorContextResolver.requireCurrent(), request));
    }

    @PostMapping("/registration/cancel/{id}")
    public Result<RegistrationSummary> cancelRegistration(@PathVariable Long id,
                                                           @Valid @RequestBody(required = false) RegistrationCancelRequest request) {
        return Result.success(workflowService.cancelRegistration(ActorContextResolver.requireCurrent(), id, request));
    }

    @GetMapping("/registration/list")
    public Result<List<RegistrationSummary>> listRegistrations() {
        return Result.success(workflowService.listPatientRegistrations(ActorContextResolver.requireCurrent()));
    }

    @GetMapping("/consultation/{registrationId}/workspace")
    public Result<ConsultationWorkspace> workspace(@PathVariable Long registrationId) {
        return Result.success(workflowService.getConsultationWorkspace(ActorContextResolver.requireCurrent(), registrationId));
    }

    @PostMapping("/consultation/{registrationId}/begin")
    public Result<RegistrationSummary> beginConsultation(@PathVariable Long registrationId) {
        return Result.success(workflowService.startConsultation(ActorContextResolver.requireCurrent(), registrationId));
    }

    @PostMapping("/consultation/{registrationId}/complete")
    public Result<RegistrationSummary> completeConsultation(@PathVariable Long registrationId) {
        return Result.success(workflowService.completeConsultation(ActorContextResolver.requireCurrent(), registrationId));
    }

    @GetMapping("/doctor/queue")
    public Result<List<RegistrationSummary>> doctorQueue() {
        return Result.success(workflowService.listDoctorQueue(ActorContextResolver.requireCurrent()));
    }

    @PostMapping("/medical-record/generate")
    public Result<MedicalRecordSummary> generateMedicalRecord(@Valid @RequestBody MedicalRecordGenerateRequest request) {
        return Result.success(workflowService.generateMedicalRecord(ActorContextResolver.requireCurrent(), request));
    }

    @PostMapping("/medical-record/save")
    public Result<MedicalRecordSummary> saveMedicalRecord(@Valid @RequestBody MedicalRecordSaveRequest request) {
        return Result.success(workflowService.saveMedicalRecord(ActorContextResolver.requireCurrent(), request));
    }

    @GetMapping("/medical-record/list/patient")
    public Result<List<MedicalRecordSummary>> listPatientMedicalRecords() {
        return Result.success(workflowService.listPatientMedicalRecords(ActorContextResolver.requireCurrent()));
    }

    @GetMapping("/medical-record/list/patient/{id}")
    public Result<List<MedicalRecordSummary>> listMedicalRecordsForPatient(@PathVariable Long id) {
        return Result.success(workflowService.listMedicalRecordsForPatient(ActorContextResolver.requireCurrent(), id));
    }

    @GetMapping("/medical-record/list/doctor")
    public Result<List<MedicalRecordSummary>> listDoctorMedicalRecords(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Result.success(workflowService.listDoctorMedicalRecords(ActorContextResolver.requireCurrent()));
        }
        return Result.success(workflowService.searchDoctorMedicalRecords(ActorContextResolver.requireCurrent(), keyword));
    }

    @GetMapping("/medical-record/{id}")
    public Result<MedicalRecordSummary> getMedicalRecord(@PathVariable Long id) {
        return Result.success(workflowService.getMedicalRecord(ActorContextResolver.requireCurrent(), id));
    }

    @PostMapping("/diagnosis/suggest")
    public Result<DiagnosisSuggestionResponse> suggestDiagnosis(@Valid @RequestBody DiagnosisSuggestionRequest request) {
        return Result.success(workflowService.suggestDiagnosis(ActorContextResolver.requireCurrent(), request));
    }

    @PatchMapping("/diagnosis/{id}/adopt")
    public Result<DiagnosisSuggestionResponse> adoptDiagnosis(@PathVariable Long id,
                                                              @Valid @RequestBody DiagnosisSuggestionAdoptRequest request) {
        return Result.success(workflowService.adoptDiagnosisSuggestion(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/diagnosis/{id}/ignore")
    public Result<DiagnosisSuggestionResponse> ignoreDiagnosis(@PathVariable Long id,
                                                               @Valid @RequestBody(required = false) DiagnosisSuggestionIgnoreRequest request) {
        return Result.success(workflowService.ignoreDiagnosisSuggestion(ActorContextResolver.requireCurrent(), id, request));
    }

    @PostMapping("/prescription/check")
    public Result<PrescriptionReviewResponse> reviewPrescription(@Valid @RequestBody PrescriptionReviewRequest request) {
        return Result.success(workflowService.reviewPrescription(ActorContextResolver.requireCurrent(), request));
    }

    @PostMapping("/prescription/submit")
    public Result<PrescriptionSummary> submitPrescription(@Valid @RequestBody PrescriptionSubmitRequest request) {
        return Result.success(workflowService.submitPrescription(ActorContextResolver.requireCurrent(), request));
    }

    @GetMapping("/prescription/list/patient")
    public Result<List<PrescriptionSummary>> listPatientPrescriptions() {
        return Result.success(workflowService.listPatientPrescriptions(ActorContextResolver.requireCurrent()));
    }

    @GetMapping("/prescription/list/doctor")
    public Result<List<PrescriptionSummary>> listDoctorPrescriptions() {
        return Result.success(workflowService.listDoctorPrescriptions(ActorContextResolver.requireCurrent()));
    }

    @GetMapping("/prescription/{id}")
    public Result<PrescriptionSummary> getPrescription(@PathVariable Long id) {
        return Result.success(workflowService.getPrescription(ActorContextResolver.requireCurrent(), id));
    }

    @GetMapping("/prescription/{id}/review")
    public Result<PrescriptionReviewResponse> getPrescriptionReview(@PathVariable Long id) {
        return Result.success(workflowService.getPrescriptionReview(ActorContextResolver.requireCurrent(), id));
    }

    @GetMapping("/feedback/list")
    public Result<List<FeedbackResponse>> listFeedback() {
        return Result.success(workflowService.listPatientFeedback(ActorContextResolver.requireCurrent()));
    }

    @PostMapping("/feedback/create")
    public Result<FeedbackResponse> createFeedback(@Valid @RequestBody FeedbackCreateRequest request) {
        return Result.success(workflowService.createFeedback(ActorContextResolver.requireCurrent(), request));
    }

    @GetMapping("/dashboard/overview")
    public Result<DashboardOverview> overview() {
        return Result.success(workflowService.overview(ActorContextResolver.requireCurrent()));
    }

    @GetMapping("/dashboard/trends")
    public Result<List<DashboardTrendPoint>> dashboardTrends(@RequestParam(required = false) LocalDate startDate,
                                                             @RequestParam(required = false) LocalDate endDate) {
        return Result.success(workflowService.dashboardTrends(ActorContextResolver.requireCurrent(), startDate, endDate));
    }

    @GetMapping("/dashboard/ai-usage")
    public Result<AiUsageStats> dashboardAiUsage(@RequestParam(required = false) String taskType) {
        return Result.success(workflowService.dashboardAiUsage(ActorContextResolver.requireCurrent(), taskType));
    }

    @GetMapping("/dashboard/prescription-review-rate")
    public Result<PrescriptionReviewRate> dashboardPrescriptionReviewRate() {
        return Result.success(workflowService.dashboardPrescriptionReviewRate(ActorContextResolver.requireCurrent()));
    }

    @GetMapping("/dashboard/risk-distribution")
    public Result<RiskDistribution> dashboardRiskDistribution() {
        return Result.success(workflowService.dashboardRiskDistribution(ActorContextResolver.requireCurrent()));
    }

    @GetMapping("/dashboard/triage-accuracy")
    public Result<TriageAccuracyStats> dashboardTriageAccuracy() {
        return Result.success(workflowService.dashboardTriageAccuracy(ActorContextResolver.requireCurrent()));
    }

    @PostMapping("/ai-stream-sessions")
    public Result<AiStreamSessionCreateResponse> createAiStreamSession(@Valid @RequestBody AiStreamSessionCreateRequest request) {
        return Result.success(aiStreamSessionService.createSession(ActorContextResolver.requireCurrent(), request));
    }

    @GetMapping("/ai-stream-sessions/{id}/events")
    public SseEmitter subscribeAiStreamSession(@PathVariable String id, @RequestParam String token) {
        return aiStreamSessionService.subscribe(id, token);
    }

    @DeleteMapping("/ai-stream-sessions/{id}")
    public Result<Void> cancelAiStreamSession(@PathVariable String id) {
        aiStreamSessionService.cancel(ActorContextResolver.requireCurrent(), id);
        return Result.success(null);
    }

    @GetMapping("/admin/audit-logs")
    public Result<List<AuditLogSummary>> listAuditLogs() {
        return Result.success(auditLogRepository.findTop50ByOrderByCreatedAtDesc().stream().map(log -> new AuditLogSummary(
                log.getId(),
                log.getActorId(),
                log.getActorRole(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getTraceId(),
                log.getSuccess(),
                log.getMessage(),
                log.getOccurredAt()
        )).toList());
    }

    @GetMapping("/admin/ai-records")
    public Result<List<AiCallRecordSummary>> listAiRecords(@RequestParam(required = false) String taskType) {
        String normalizedTaskType = taskType == null || taskType.isBlank() ? null : taskType.trim().toUpperCase().replace("-", "_");
        if ("ALL".equals(normalizedTaskType)) {
            normalizedTaskType = null;
        }
        var records = normalizedTaskType == null
                ? aiCallRecordRepository.findAllByOrderByCreatedAtDesc()
                : aiCallRecordRepository.findByTaskTypeOrderByCreatedAtDesc(normalizedTaskType);
        return Result.success(records.stream().map(record -> new AiCallRecordSummary(
                record.getId(),
                record.getTaskType(),
                record.getBusinessRecordId(),
                record.getOperatorId(),
                record.getOperatorRole(),
                record.getProvider(),
                record.getModelName(),
                record.getConfigVersion(),
                record.getPromptVersion(),
                record.getInputSummary(),
                record.getOutputSummary(),
                record.getCallStatus(),
                record.getErrorSummary(),
                record.getDurationMs(),
                record.getTraceId(),
                record.getDegraded(),
                record.getRetryCount(),
                record.getCreatedAt(),
                record.getOutputSummary()
        )).toList());
    }

    @GetMapping("/notifications/unread")
    public Result<List<NotificationRecordSummary>> listUnreadNotifications() {
        return Result.success(workflowService.listUnreadNotifications(ActorContextResolver.requireCurrent()));
    }

    @PutMapping("/notifications/{id}/read")
    public Result<NotificationRecordSummary> markNotificationRead(@PathVariable Long id) {
        return Result.success(workflowService.markNotificationRead(ActorContextResolver.requireCurrent(), id));
    }
}
