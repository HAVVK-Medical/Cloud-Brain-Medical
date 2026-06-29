package com.cloudbrain.dto.workflow;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class WorkflowDtos {

    private WorkflowDtos() {
    }

    public record DepartmentOption(
            Long id,
            String code,
            String name,
            String type,
            String description
    ) {
    }

    public record DoctorOption(
            Long id,
            String username,
            String name,
            Long departmentId,
            String departmentName,
            String title,
            String specialty,
            String introduction
    ) {
    }

    public record ScheduleOption(
            Long id,
            Long doctorId,
            String doctorName,
            Long departmentId,
            String departmentName,
            LocalDate workDate,
            String period,
            Integer totalSlots,
            Integer remainingSlots,
            String visitLevel,
            String status
    ) {
    }

    public record DrugOption(
            Long id,
            String code,
            String name,
            String pinyinCode,
            String specification,
            String dosageForm,
            String packageUnit,
            String manufacturer,
            BigDecimal unitPrice,
            String defaultUsage,
            String contraindications,
            String indications
    ) {
    }

    public record TriageRequest(
            @NotBlank @Size(max = 1000) String chiefComplaint,
            List<AiContentAttachment> attachments
    ) {
        public TriageRequest(String chiefComplaint) {
            this(chiefComplaint, List.of());
        }
    }

    public record TriageResponse(
            Long triageRecordId,
            String chiefComplaint,
            Long recommendedDepartmentId,
            String recommendedDept,
            List<DoctorOption> recommendedDoctors,
            List<ScheduleOption> availableSchedules,
            String reason,
            String callStatus,
            String recommendationSource,
            Long aiRecommendedDepartmentId,
            String aiRecommendedDept,
            boolean degraded
    ) {
    }

    public record ConversationTriageConfirmRequest(
            @NotBlank @Size(max = 1000) String chiefComplaint,
            @Size(max = 128) String department,
            @Size(max = 64) String departmentCode,
            @Size(max = 2000) String reason,
            @Size(max = 32) String urgencyLevel
    ) {
    }

    public record RegistrationCreateRequest(
            @NotNull Long scheduleId,
            Long triageRecordId
    ) {
    }

    public record RegistrationCancelRequest(
            @Size(max = 255) String reason
    ) {
    }

    public record RegistrationSummary(
            Long id,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            Long departmentId,
            String departmentName,
            Long scheduleId,
            LocalDate workDate,
            String period,
            String visitLevel,
            String status,
            Long triageRecordId,
            String chiefComplaint,
            LocalDateTime registrationTime,
            LocalDateTime consultationStartTime,
            LocalDateTime recordConfirmedTime,
            LocalDateTime prescriptionSubmittedTime,
            LocalDateTime completedTime,
            Long medicalRecordId,
            Long prescriptionId,
            String riskLevel
    ) {
    }

    public record MedicalRecordGenerateRequest(
            @NotNull Long registrationId,
            @NotBlank @Size(max = 4000) String conversationText,
            @Size(max = 1000) String diagnosisDirection,
            List<AiContentAttachment> attachments
    ) {
        public MedicalRecordGenerateRequest(Long registrationId, String conversationText, String diagnosisDirection) {
            this(registrationId, conversationText, diagnosisDirection, List.of());
        }
    }

    public record MedicalRecordSaveRequest(
            @NotNull Long registrationId,
            @Size(max = 4000) String conversationText,
            @Size(max = 1000) String chiefComplaint,
            @Size(max = 2000) String presentIllness,
            @Size(max = 2000) String pastHistory,
            @Size(max = 2000) String physicalExam,
            @Size(max = 2000) String preliminaryDiagnosis,
            @Size(max = 2000) String treatmentPlan,
            @Size(max = 2000) String docNote,
            Boolean aiGenerated
    ) {
    }

    public record MedicalRecordSummary(
            Long id,
            Long registrationId,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            String departmentName,
            String chiefComplaint,
            String presentIllness,
            String pastHistory,
            String physicalExam,
            String preliminaryDiagnosis,
            String treatmentPlan,
            String conversationText,
            String docNote,
            Boolean aiGenerated,
            Integer version,
            Instant createdAt,
            boolean degraded
    ) {
    }

    public record DiagnosisSuggestionRequest(
            @NotNull Long registrationId,
            @NotBlank @Size(max = 4000) String conversationText,
            @Size(max = 1000) String diagnosisDirection,
            List<AiContentAttachment> attachments
    ) {
        public DiagnosisSuggestionRequest(Long registrationId, String conversationText, String diagnosisDirection) {
            this(registrationId, conversationText, diagnosisDirection, List.of());
        }
    }

    public record DiagnosisSuggestionResponse(
            Long id,
            Long registrationId,
            String suggestedDiagnoses,
            String suggestedExamItems,
            String adoptionStatus,
            String summary,
            boolean degraded
    ) {
    }

    public record DiagnosisSuggestionAdoptRequest(
            @NotBlank @Size(max = 1000) String finalDiagnosis
    ) {
    }

    public record DiagnosisSuggestionIgnoreRequest(
            @Size(max = 1000) String reason
    ) {
    }

    public record PrescriptionItemRequest(
            @NotNull Long drugId,
            @NotNull @Min(0) BigDecimal dosage,
            @NotBlank @Size(max = 64) String frequency,
            @NotBlank @Size(max = 64) String duration,
            @NotNull @Min(1) @Max(999) Integer quantity,
            @Size(max = 1000) String usageInstruction
    ) {
    }

    public record PrescriptionReviewRequest(
            @NotNull Long registrationId,
            @Valid @NotEmpty List<PrescriptionItemRequest> items,
            List<AiContentAttachment> attachments
    ) {
        public PrescriptionReviewRequest(Long registrationId, List<PrescriptionItemRequest> items) {
            this(registrationId, items, List.of());
        }
    }

    public record PrescriptionSubmitRequest(
            @NotNull Long registrationId,
            @NotNull Long reviewId,
            @Valid @NotEmpty List<PrescriptionItemRequest> items,
            @Size(max = 1000) String manualConfirmation
    ) {
    }

    public record PrescriptionItemSummary(
            Long id,
            Long drugId,
            String drugName,
            String specification,
            String dosageForm,
            String packageUnit,
            BigDecimal unitPrice,
            BigDecimal dosage,
            String frequency,
            String duration,
            Integer quantity,
            String usageInstruction
    ) {
    }

    public record PrescriptionRuleHit(
            Long ruleId,
            String ruleCode,
            String ruleType,
            String riskLevel,
            String alertMessage,
            String suggestion,
            String basisSnapshot
    ) {
    }

    public record PrescriptionReviewResponse(
            Long reviewId,
            Long registrationId,
            Long prescriptionId,
            String reviewStatus,
            String riskLevel,
            String localRuleHits,
            List<PrescriptionRuleHit> ruleHits,
            String ruleEngineStatus,
            String contextMissingItems,
            List<String> contextMissingItemList,
            String llmSuggestion,
            String llmSummary,
            String llmCallStatus,
            String prescriptionSnapshotHash,
            String reviewContextHash,
            Boolean degraded,
            String bindStatus,
            List<PrescriptionItemSummary> items
    ) {
        public PrescriptionReviewResponse(Long reviewId,
                                          Long registrationId,
                                          Long prescriptionId,
                                          String riskLevel,
                                          String localRuleHits,
                                          String ruleEngineStatus,
                                          String llmSuggestion,
                                          String llmSummary,
                                          String bindStatus,
                                          List<PrescriptionItemSummary> items) {
            this(
                    reviewId,
                    registrationId,
                    prescriptionId,
                    "BOUND".equalsIgnoreCase(bindStatus) ? "BOUND" : "UNBOUND",
                    riskLevel,
                    localRuleHits,
                    List.of(),
                    ruleEngineStatus,
                    "",
                    List.of(),
                    llmSuggestion,
                    llmSummary,
                    "LOCAL_SIMULATED",
                    null,
                    null,
                    false,
                    bindStatus,
                    items
            );
        }
    }

    public record PrescriptionSummary(
            Long id,
            Long registrationId,
            Long patientId,
            String patientName,
            Long doctorId,
            String doctorName,
            String departmentName,
            String status,
            String riskLevel,
            Long reviewId,
            List<PrescriptionItemSummary> items,
            PrescriptionReviewResponse review,
            Instant createdAt
    ) {
    }

    public record FeedbackCreateRequest(
            @NotNull Long registrationId,
            @NotNull @Min(1) @Max(5) Integer rating,
            Boolean triageAccurate,
            @Size(max = 1000) String comment
    ) {
    }

    public record FeedbackResponse(
            Long id,
            Long registrationId,
            Integer rating,
            Boolean triageAccurate,
            String comment,
            Instant createdAt
    ) {
    }

    public record DashboardOverview(
            long todayRegistrations,
            long todayVisits,
            long waitingRegistrations,
            long completedRegistrations,
            long todayPrescriptions,
            long todayAiCallRecords,
            long medicalRecords,
            long prescriptions,
            long highRiskReviews,
            long feedbackCount,
            long aiCallRecords,
            Instant updatedAt
    ) {
        public DashboardOverview(long todayRegistrations,
                                 long waitingRegistrations,
                                 long completedRegistrations,
                                 long medicalRecords,
                                 long prescriptions,
                                 long highRiskReviews,
                                 long feedbackCount,
                                 long aiCallRecords,
                                 Instant updatedAt) {
            this(
                    todayRegistrations,
                    todayRegistrations,
                    waitingRegistrations,
                    completedRegistrations,
                    prescriptions,
                    aiCallRecords,
                    medicalRecords,
                    prescriptions,
                    highRiskReviews,
                    feedbackCount,
                    aiCallRecords,
                    updatedAt
            );
        }
    }

    public record DashboardTrendPoint(
            LocalDate date,
            long registrations,
            long visits,
            long prescriptions,
            long aiCalls
    ) {
    }

    public record AiUsageBucket(
            String taskType,
            long calls,
            long successCalls,
            long failedCalls,
            long degradedCalls,
            double usageRate,
            long averageDurationMs
    ) {
    }

    public record AiUsageStats(
            long totalCalls,
            long successCalls,
            long failedCalls,
            long degradedCalls,
            double successRate,
            long averageDurationMs,
            List<AiUsageBucket> buckets,
            Instant updatedAt
    ) {
    }

    public record PrescriptionReviewRate(
            long totalReviews,
            long lowRiskReviews,
            long mediumRiskReviews,
            long highRiskReviews,
            long manualRequiredReviews,
            long unknownReviews,
            double passRate,
            Instant updatedAt
    ) {
    }

    public record RiskDistributionBucket(
            String riskLevel,
            long count,
            double ratio
    ) {
    }

    public record RiskDistribution(
            long totalReviews,
            List<RiskDistributionBucket> buckets,
            Instant updatedAt
    ) {
    }

    public record TriageAccuracyStats(
            long feedbackCount,
            long accurateCount,
            long inaccurateCount,
            long noFeedbackCount,
            double accuracyRate,
            long sampleCount,
            Instant updatedAt
    ) {
    }

    public record AiStreamSessionCreateRequest(
            @NotBlank @Size(max = 32) String taskType,
            @NotNull Long registrationId,
            @NotBlank @Size(max = 4000) String conversationText,
            @Size(max = 1000) String diagnosisDirection,
            List<AiContentAttachment> attachments
    ) {
        public AiStreamSessionCreateRequest(String taskType,
                                            Long registrationId,
                                            String conversationText,
                                            String diagnosisDirection) {
            this(taskType, registrationId, conversationText, diagnosisDirection, List.of());
        }
    }

    public record AiStreamSessionCreateResponse(
            String sessionId,
            String streamToken,
            String taskType,
            Instant expiresAt
    ) {
    }

    public record ConsultationWorkspace(
            RegistrationSummary registration,
            MedicalRecordSummary latestMedicalRecord,
            PrescriptionSummary latestPrescription,
            List<PrescriptionReviewResponse> recentReviews,
            List<String> nextActions
    ) {
    }

    public record PrescriptionRuleSummary(
            Long id,
            String ruleCode,
            String ruleType,
            String applicableDrugs,
            String applicableDiseases,
            String riskLevel,
            String alertMessage,
            String suggestion,
            String basis,
            Boolean seeded,
            Integer version,
            String validationStatus,
            String status,
            Instant updatedAt
    ) {
    }

    public record AiCallRecordSummary(
            Long id,
            String taskType,
            Long businessRecordId,
            Long operatorId,
            String operatorRole,
            String provider,
            String modelName,
            String configVersion,
            String promptVersion,
            String inputSummary,
            String outputSummary,
            String callStatus,
            String errorSummary,
            Long durationMs,
            String traceId,
            Boolean degraded,
            Integer retryCount,
            Instant createdAt,
            String businessSummary
    ) {
    }

    public record AiConfigSummary(
            Long id,
            String provider,
            String modelName,
            String taskScope,
            Integer timeoutSeconds,
            Boolean defaultConfig,
            String healthStatus,
            String configVersion,
            Boolean enabled,
            Integer priority,
            String status,
            Instant updatedAt
    ) {
    }

    public record AuditLogSummary(
            Long id,
            Long actorId,
            String actorRole,
            String action,
            String resourceType,
            Long resourceId,
            String traceId,
            Boolean success,
            String message,
            Instant occurredAt
    ) {
    }

    public record NotificationRecordSummary(
            Long id,
            Long recipientId,
            String recipientRole,
            String alertType,
            String statisticsBucket,
            String displayLevel,
            Long businessRecordId,
            String patientSummary,
            String riskSummary,
            Boolean read,
            Instant createdAt
    ) {
    }

    public record AiContentAttachment(
            @NotBlank @Size(max = 32) String type,
            @Size(max = 2000) String url,
            @Size(max = 2000) String data,
            @Size(max = 128) String mimeType,
            @Size(max = 32) String detail,
            @Size(max = 255) String name
    ) {
    }
}
