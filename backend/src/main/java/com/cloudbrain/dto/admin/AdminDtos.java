package com.cloudbrain.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record DepartmentRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 64) String type,
            String description,
            @NotBlank @Size(max = 32) String status
    ) {
    }

    public record DepartmentSummary(
            Long id,
            String code,
            String name,
            String type,
            String description,
            String status,
            long doctorCount,
            long activeScheduleCount,
            Instant updatedAt
    ) {
    }

    public record DoctorRequest(
            @NotBlank @Size(max = 64) String username,
            @Size(max = 255) String password,
            @NotBlank @Size(max = 64) String name,
            @NotNull Long departmentId,
            @Size(max = 64) String title,
            @Size(max = 128) String specialty,
            String introduction,
            @NotBlank @Size(max = 32) String status
    ) {
    }

    public record DoctorSummary(
            Long id,
            String username,
            String name,
            Long departmentId,
            String departmentName,
            String title,
            String specialty,
            String introduction,
            String status,
            long scheduleCount,
            Instant updatedAt
    ) {
    }

    public record ScheduleRequest(
            @NotNull Long doctorId,
            @NotNull Long departmentId,
            @NotNull LocalDate workDate,
            @NotBlank @Size(max = 32) String period,
            @NotNull @Min(1) Integer totalSlots,
            Integer remainingSlots,
            @NotBlank @Size(max = 32) String visitLevel,
            @NotBlank @Size(max = 32) String status
    ) {
    }

    public record BatchScheduleRequest(
            @NotNull Long doctorId,
            @NotNull Long departmentId,
            @NotEmpty List<@NotNull LocalDate> workDates,
            @NotEmpty List<@NotBlank String> periods,
            @NotNull @Min(1) Integer totalSlots,
            Integer remainingSlots,
            @NotBlank @Size(max = 32) String visitLevel,
            @NotBlank @Size(max = 32) String status
    ) {
    }

    public record ScheduleSummary(
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
            String status,
            Instant updatedAt
    ) {
    }

    public record DrugRequest(
            @NotBlank @Size(max = 64) String code,
            @NotBlank @Size(max = 128) String name,
            @Size(max = 128) String pinyinCode,
            @Size(max = 255) String specification,
            @Size(max = 64) String dosageForm,
            @Size(max = 32) String packageUnit,
            @Size(max = 255) String manufacturer,
            BigDecimal unitPrice,
            String defaultUsage,
            String contraindications,
            String precautions,
            String indications,
            String interactionSummary,
            @NotBlank @Size(max = 32) String status
    ) {
    }

    public record DrugSummary(
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
            String precautions,
            String indications,
            String interactionSummary,
            String status,
            Instant updatedAt
    ) {
    }

    public record PrescriptionRuleRequest(
            @NotBlank @Size(max = 128) String ruleCode,
            @NotBlank @Size(max = 64) String ruleType,
            String applicableDrugs,
            String applicableDiseases,
            String applicablePopulations,
            String conditionExpression,
            @Size(max = 32) String riskLevel,
            String alertMessage,
            String suggestion,
            String basis,
            Boolean seeded,
            String validationStatus,
            @NotBlank @Size(max = 32) String status
    ) {
    }

    public record PrescriptionRuleSummary(
            Long id,
            String ruleCode,
            String ruleType,
            String applicableDrugs,
            String applicableDiseases,
            String applicablePopulations,
            String conditionExpression,
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

    public record AiConfigRequest(
            @NotBlank @Size(max = 64) String provider,
            @NotBlank @Size(max = 128) String modelName,
            @Size(max = 255) String apiUrl,
            @Size(max = 2000) String apiKey,
            @Size(max = 64) String keyVersion,
            @NotBlank @Size(max = 64) String taskScope,
            @NotNull @Min(1) Integer timeoutSeconds,
            Boolean defaultConfig,
            @Size(max = 32) String healthStatus,
            @NotBlank @Size(max = 32) String status,
            Boolean enabled,
            @NotNull @Min(0) Integer priority,
            @Size(max = 64) String configVersion
    ) {
    }

    public record AiConfigSecretRequest(
            @Size(max = 2000) String apiKey,
            @Size(max = 64) String keyVersion
    ) {
    }

    public record AiConfigSummary(
            Long id,
            String provider,
            String modelName,
            String apiUrl,
            String taskScope,
            Integer timeoutSeconds,
            Boolean defaultConfig,
            String healthStatus,
            String configVersion,
            Boolean enabled,
            Integer priority,
            String status,
            Boolean hasApiKey,
            String keyVersion,
            Instant updatedAt
    ) {
    }

    public record PromptTemplateRequest(
            @NotBlank @Size(max = 128) String templateCode,
            @NotBlank @Size(max = 64) String taskType,
            @Size(max = 64) String deptCode,
            String templateBody,
            String variableWhitelist,
            Integer version,
            Boolean defaultTemplate,
            @NotBlank @Size(max = 32) String status
    ) {
    }

    public record PromptTemplateSummary(
            Long id,
            String templateCode,
            String taskType,
            String deptCode,
            String templateBody,
            String variableWhitelist,
            Integer version,
            Boolean defaultTemplate,
            String status,
            Instant updatedAt
    ) {
    }
}
