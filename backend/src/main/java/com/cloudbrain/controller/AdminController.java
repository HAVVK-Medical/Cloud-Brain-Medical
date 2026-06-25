package com.cloudbrain.controller;

import com.cloudbrain.application.admin.AdminService;
import com.cloudbrain.common.Result;
import com.cloudbrain.dto.admin.AdminDtos.AiConfigRequest;
import com.cloudbrain.dto.admin.AdminDtos.AiConfigSecretRequest;
import com.cloudbrain.dto.admin.AdminDtos.AiConfigSummary;
import com.cloudbrain.dto.admin.AdminDtos.BatchScheduleRequest;
import com.cloudbrain.dto.admin.AdminDtos.DepartmentRequest;
import com.cloudbrain.dto.admin.AdminDtos.DepartmentSummary;
import com.cloudbrain.dto.admin.AdminDtos.DoctorRequest;
import com.cloudbrain.dto.admin.AdminDtos.DoctorSummary;
import com.cloudbrain.dto.admin.AdminDtos.DrugRequest;
import com.cloudbrain.dto.admin.AdminDtos.DrugSummary;
import com.cloudbrain.dto.admin.AdminDtos.PromptTemplateRequest;
import com.cloudbrain.dto.admin.AdminDtos.PromptTemplateSummary;
import com.cloudbrain.dto.admin.AdminDtos.PrescriptionRuleRequest;
import com.cloudbrain.dto.admin.AdminDtos.PrescriptionRuleSummary;
import com.cloudbrain.dto.admin.AdminDtos.ScheduleRequest;
import com.cloudbrain.dto.admin.AdminDtos.ScheduleSummary;
import com.cloudbrain.security.ActorContextResolver;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/departments")
    public Result<List<DepartmentSummary>> listDepartments() {
        return Result.success(adminService.listDepartments(ActorContextResolver.requireCurrent()));
    }

    @PostMapping("/departments")
    public Result<DepartmentSummary> createDepartment(@Valid @RequestBody DepartmentRequest request) {
        return Result.success(adminService.createDepartment(ActorContextResolver.requireCurrent(), request));
    }

    @PutMapping("/departments/{id}")
    public Result<DepartmentSummary> updateDepartment(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return Result.success(adminService.updateDepartment(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/departments/{id}/toggle")
    public Result<DepartmentSummary> toggleDepartment(@PathVariable Long id) {
        return Result.success(adminService.toggleDepartmentStatus(ActorContextResolver.requireCurrent(), id));
    }

    @GetMapping("/doctors")
    public Result<List<DoctorSummary>> listDoctors(@RequestParam(required = false) Long departmentId) {
        return Result.success(adminService.listDoctors(ActorContextResolver.requireCurrent(), departmentId));
    }

    @PostMapping("/doctors")
    public Result<DoctorSummary> createDoctor(@Valid @RequestBody DoctorRequest request) {
        return Result.success(adminService.createDoctor(ActorContextResolver.requireCurrent(), request));
    }

    @PutMapping("/doctors/{id}")
    public Result<DoctorSummary> updateDoctor(@PathVariable Long id, @Valid @RequestBody DoctorRequest request) {
        return Result.success(adminService.updateDoctor(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/doctors/{id}/toggle")
    public Result<DoctorSummary> toggleDoctor(@PathVariable Long id) {
        return Result.success(adminService.toggleDoctorStatus(ActorContextResolver.requireCurrent(), id));
    }

    @GetMapping("/schedules")
    public Result<List<ScheduleSummary>> listSchedules(@RequestParam(required = false) Long doctorId,
                                                       @RequestParam(required = false) Long departmentId,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return Result.success(adminService.listSchedules(ActorContextResolver.requireCurrent(), doctorId, departmentId, from, to));
    }

    @PostMapping("/schedules")
    public Result<ScheduleSummary> createSchedule(@Valid @RequestBody ScheduleRequest request) {
        return Result.success(adminService.createSchedule(ActorContextResolver.requireCurrent(), request));
    }

    @PutMapping("/schedules/{id}")
    public Result<ScheduleSummary> updateSchedule(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return Result.success(adminService.updateSchedule(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/schedules/{id}/toggle")
    public Result<ScheduleSummary> toggleSchedule(@PathVariable Long id) {
        return Result.success(adminService.toggleScheduleStatus(ActorContextResolver.requireCurrent(), id));
    }

    @PostMapping("/schedules/batch")
    public Result<List<ScheduleSummary>> batchCreateSchedules(@Valid @RequestBody BatchScheduleRequest request) {
        return Result.success(adminService.batchCreateSchedules(ActorContextResolver.requireCurrent(), request));
    }

    @GetMapping("/drugs")
    public Result<List<DrugSummary>> listDrugs(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status) {
        return Result.success(adminService.listDrugs(ActorContextResolver.requireCurrent(), keyword, status));
    }

    @PostMapping("/drugs")
    public Result<DrugSummary> createDrug(@Valid @RequestBody DrugRequest request) {
        return Result.success(adminService.createDrug(ActorContextResolver.requireCurrent(), request));
    }

    @PutMapping("/drugs/{id}")
    public Result<DrugSummary> updateDrug(@PathVariable Long id, @Valid @RequestBody DrugRequest request) {
        return Result.success(adminService.updateDrug(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/drugs/{id}/toggle")
    public Result<DrugSummary> toggleDrug(@PathVariable Long id) {
        return Result.success(adminService.toggleDrugStatus(ActorContextResolver.requireCurrent(), id));
    }

    @GetMapping("/prescription-rules")
    public Result<List<PrescriptionRuleSummary>> listRules(@RequestParam(required = false) String ruleType,
                                                           @RequestParam(required = false) String status) {
        return Result.success(adminService.listRules(ActorContextResolver.requireCurrent(), ruleType, status));
    }

    @PostMapping("/prescription-rules")
    public Result<PrescriptionRuleSummary> createRule(@Valid @RequestBody PrescriptionRuleRequest request) {
        return Result.success(adminService.createRule(ActorContextResolver.requireCurrent(), request));
    }

    @PutMapping("/prescription-rules/{id}")
    public Result<PrescriptionRuleSummary> updateRule(@PathVariable Long id, @Valid @RequestBody PrescriptionRuleRequest request) {
        return Result.success(adminService.updateRule(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/prescription-rules/{id}/toggle")
    public Result<PrescriptionRuleSummary> toggleRule(@PathVariable Long id) {
        return Result.success(adminService.toggleRuleStatus(ActorContextResolver.requireCurrent(), id));
    }

    @GetMapping("/ai-config")
    public Result<List<AiConfigSummary>> listAiConfig() {
        return Result.success(adminService.listAiConfigs(ActorContextResolver.requireCurrent()));
    }

    @PostMapping("/ai-config")
    public Result<AiConfigSummary> createAiConfig(@Valid @RequestBody AiConfigRequest request) {
        return Result.success(adminService.createAiConfig(ActorContextResolver.requireCurrent(), request));
    }

    @PutMapping("/ai-config/{id}")
    public Result<AiConfigSummary> updateAiConfig(@PathVariable Long id, @Valid @RequestBody AiConfigRequest request) {
        return Result.success(adminService.updateAiConfig(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/ai-config/{id}/toggle")
    public Result<AiConfigSummary> toggleAiConfig(@PathVariable Long id) {
        return Result.success(adminService.toggleAiConfigStatus(ActorContextResolver.requireCurrent(), id));
    }

    @PatchMapping("/ai-config/{id}/rotate-key")
    public Result<AiConfigSummary> rotateAiConfigKey(@PathVariable Long id, @Valid @RequestBody AiConfigSecretRequest request) {
        return Result.success(adminService.rotateAiConfigKey(
                ActorContextResolver.requireCurrent(),
                id,
                request.apiKey(),
                request.keyVersion()
        ));
    }

    @GetMapping("/prompt-templates")
    public Result<List<PromptTemplateSummary>> listPromptTemplates() {
        return Result.success(adminService.listPromptTemplates(ActorContextResolver.requireCurrent()));
    }

    @PostMapping("/prompt-templates")
    public Result<PromptTemplateSummary> createPromptTemplate(@Valid @RequestBody PromptTemplateRequest request) {
        return Result.success(adminService.createPromptTemplate(ActorContextResolver.requireCurrent(), request));
    }

    @PutMapping("/prompt-templates/{id}")
    public Result<PromptTemplateSummary> updatePromptTemplate(@PathVariable Long id, @Valid @RequestBody PromptTemplateRequest request) {
        return Result.success(adminService.updatePromptTemplate(ActorContextResolver.requireCurrent(), id, request));
    }

    @PatchMapping("/prompt-templates/{id}/toggle")
    public Result<PromptTemplateSummary> togglePromptTemplate(@PathVariable Long id) {
        return Result.success(adminService.togglePromptTemplateStatus(ActorContextResolver.requireCurrent(), id));
    }
}
