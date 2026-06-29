package com.cloudbrain.application.admin;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.dto.admin.AdminDtos.AiConfigRequest;
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
import com.cloudbrain.entity.auth.DoctorEntity;
import com.cloudbrain.entity.core.AIConfigEntity;
import com.cloudbrain.entity.core.DepartmentEntity;
import com.cloudbrain.entity.core.DrugEntity;
import com.cloudbrain.entity.core.PromptTemplateEntity;
import com.cloudbrain.entity.core.PrescriptionRuleDefinitionEntity;
import com.cloudbrain.entity.core.ScheduleEntity;
import com.cloudbrain.repository.AIConfigJpaRepository;
import com.cloudbrain.repository.AuditLogJpaRepository;
import com.cloudbrain.repository.DepartmentJpaRepository;
import com.cloudbrain.repository.DoctorJpaRepository;
import com.cloudbrain.repository.DrugJpaRepository;
import com.cloudbrain.repository.PromptTemplateJpaRepository;
import com.cloudbrain.repository.PrescriptionRuleDefinitionJpaRepository;
import com.cloudbrain.repository.ScheduleJpaRepository;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.RolePolicy;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final DepartmentJpaRepository departmentRepository;
    private final DoctorJpaRepository doctorRepository;
    private final ScheduleJpaRepository scheduleRepository;
    private final DrugJpaRepository drugRepository;
    private final PrescriptionRuleDefinitionJpaRepository ruleRepository;
    private final AIConfigJpaRepository aiConfigRepository;
    private final PromptTemplateJpaRepository promptTemplateRepository;
    private final AuditLogJpaRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ConfigCipher configCipher;
    private final RolePolicy rolePolicy;

    public AdminService(DepartmentJpaRepository departmentRepository,
                        DoctorJpaRepository doctorRepository,
                        ScheduleJpaRepository scheduleRepository,
                        DrugJpaRepository drugRepository,
                        PrescriptionRuleDefinitionJpaRepository ruleRepository,
                        AIConfigJpaRepository aiConfigRepository,
                        PromptTemplateJpaRepository promptTemplateRepository,
                        AuditLogJpaRepository auditLogRepository,
                        PasswordEncoder passwordEncoder,
                        ConfigCipher configCipher,
                        RolePolicy rolePolicy) {
        this.departmentRepository = departmentRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.drugRepository = drugRepository;
        this.ruleRepository = ruleRepository;
        this.aiConfigRepository = aiConfigRepository;
        this.promptTemplateRepository = promptTemplateRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.configCipher = configCipher;
        this.rolePolicy = rolePolicy;
    }

    @Transactional
    public List<DepartmentSummary> listDepartments(ActorContext actorContext) {
        requireAdmin(actorContext);
        return departmentRepository.findAll().stream()
                .sorted(Comparator.comparing(DepartmentEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDepartmentSummary)
                .toList();
    }

    @Transactional
    public DepartmentSummary createDepartment(ActorContext actorContext, DepartmentRequest request) {
        requireAdmin(actorContext);
        if (departmentRepository.findByCode(request.code()).isPresent()) {
            throw conflict("department code already exists");
        }
        DepartmentEntity entity = new DepartmentEntity();
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setType(blankToNull(request.type()));
        entity.setDescription(blankToNull(request.description()));
        entity.setStatus(normalizeStatus(request.status()));
        return toDepartmentSummary(departmentRepository.save(entity));
    }

    @Transactional
    public DepartmentSummary updateDepartment(ActorContext actorContext, Long id, DepartmentRequest request) {
        requireAdmin(actorContext);
        DepartmentEntity entity = requireDepartment(id);
        if (!Objects.equals(entity.getCode(), request.code()) && departmentRepository.findByCode(request.code()).isPresent()) {
            throw conflict("department code already exists");
        }
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setType(blankToNull(request.type()));
        entity.setDescription(blankToNull(request.description()));
        entity.setStatus(normalizeStatus(request.status()));
        return toDepartmentSummary(departmentRepository.save(entity));
    }

    @Transactional
    public DepartmentSummary toggleDepartmentStatus(ActorContext actorContext, Long id) {
        requireAdmin(actorContext);
        DepartmentEntity entity = requireDepartment(id);
        entity.setStatus(isActive(entity.getStatus()) ? INACTIVE : ACTIVE);
        return toDepartmentSummary(departmentRepository.save(entity));
    }

    @Transactional
    public List<DoctorSummary> listDoctors(ActorContext actorContext, Long departmentId) {
        requireAdmin(actorContext);
        List<DoctorEntity> doctors = departmentId == null
                ? doctorRepository.findAll()
                : doctorRepository.findAll().stream()
                        .filter(doctor -> Objects.equals(doctor.getDepartmentId(), departmentId))
                        .toList();
        return doctors.stream()
                .sorted(Comparator.comparing(DoctorEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDoctorSummary)
                .toList();
    }

    @Transactional
    public DoctorSummary createDoctor(ActorContext actorContext, DoctorRequest request) {
        requireAdmin(actorContext);
        if (doctorRepository.existsByUsername(request.username())) {
            throw conflict("doctor username already exists");
        }
        DepartmentEntity department = requireDepartment(request.departmentId());
        DoctorEntity entity = new DoctorEntity();
        entity.setUsername(request.username().trim());
        entity.setPasswordHash(passwordEncoder.encode(firstNonBlank(request.password(), "doctor123")));
        entity.setName(request.name().trim());
        entity.setDepartmentId(department.getId());
        entity.setTitle(blankToNull(request.title()));
        entity.setSpecialty(blankToNull(request.specialty()));
        entity.setIntroduction(blankToNull(request.introduction()));
        entity.setStatus(normalizeStatus(request.status()));
        return toDoctorSummary(doctorRepository.save(entity));
    }

    @Transactional
    public DoctorSummary updateDoctor(ActorContext actorContext, Long id, DoctorRequest request) {
        requireAdmin(actorContext);
        DoctorEntity entity = requireDoctor(id);
        DepartmentEntity department = requireDepartment(request.departmentId());
        if (!Objects.equals(entity.getUsername(), request.username()) && doctorRepository.existsByUsername(request.username())) {
            throw conflict("doctor username already exists");
        }
        entity.setUsername(request.username().trim());
        if (request.password() != null && !request.password().isBlank()) {
            entity.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        entity.setName(request.name().trim());
        entity.setDepartmentId(department.getId());
        entity.setTitle(blankToNull(request.title()));
        entity.setSpecialty(blankToNull(request.specialty()));
        entity.setIntroduction(blankToNull(request.introduction()));
        entity.setStatus(normalizeStatus(request.status()));
        return toDoctorSummary(doctorRepository.save(entity));
    }

    @Transactional
    public DoctorSummary toggleDoctorStatus(ActorContext actorContext, Long id) {
        requireAdmin(actorContext);
        DoctorEntity entity = requireDoctor(id);
        entity.setStatus(isActive(entity.getStatus()) ? INACTIVE : ACTIVE);
        return toDoctorSummary(doctorRepository.save(entity));
    }

    @Transactional
    public List<ScheduleSummary> listSchedules(ActorContext actorContext, Long doctorId, Long departmentId, LocalDate from, LocalDate to) {
        requireAdmin(actorContext);
        return scheduleRepository.findAll().stream()
                .filter(schedule -> doctorId == null || Objects.equals(schedule.getDoctorId(), doctorId))
                .filter(schedule -> departmentId == null || Objects.equals(schedule.getDepartmentId(), departmentId))
                .filter(schedule -> from == null || !schedule.getWorkDate().isBefore(from))
                .filter(schedule -> to == null || !schedule.getWorkDate().isAfter(to))
                .sorted(Comparator.comparing(ScheduleEntity::getWorkDate).thenComparing(ScheduleEntity::getPeriod))
                .map(this::toScheduleSummary)
                .toList();
    }

    @Transactional
    public ScheduleSummary createSchedule(ActorContext actorContext, ScheduleRequest request) {
        requireAdmin(actorContext);
        return toScheduleSummary(saveSchedule(new ScheduleEntity(), request));
    }

    @Transactional
    public ScheduleSummary updateSchedule(ActorContext actorContext, Long id, ScheduleRequest request) {
        requireAdmin(actorContext);
        ScheduleEntity entity = requireSchedule(id);
        if (request.remainingSlots() != null && request.remainingSlots() > request.totalSlots()) {
            throw conflict("remaining slots cannot exceed total slots");
        }
        return toScheduleSummary(saveSchedule(entity, request));
    }

    @Transactional
    public ScheduleSummary toggleScheduleStatus(ActorContext actorContext, Long id) {
        requireAdmin(actorContext);
        ScheduleEntity entity = requireSchedule(id);
        entity.setStatus(isActive(entity.getStatus()) ? INACTIVE : ACTIVE);
        return toScheduleSummary(scheduleRepository.save(entity));
    }

    @Transactional
    public List<ScheduleSummary> batchCreateSchedules(ActorContext actorContext, BatchScheduleRequest request) {
        requireAdmin(actorContext);
        requireDoctor(request.doctorId());
        requireDepartment(request.departmentId());
        List<ScheduleSummary> summaries = new ArrayList<>();
        for (LocalDate workDate : request.workDates()) {
            for (String period : request.periods()) {
                ScheduleRequest scheduleRequest = new ScheduleRequest(
                        request.doctorId(),
                        request.departmentId(),
                        workDate,
                        period,
                        request.totalSlots(),
                        request.remainingSlots(),
                        request.visitLevel(),
                        request.status()
                );
                summaries.add(createSchedule(actorContext, scheduleRequest));
            }
        }
        return summaries;
    }

    @Transactional
    public List<DrugSummary> listDrugs(ActorContext actorContext, String keyword, String status) {
        requireAdmin(actorContext);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedStatus = blankToNull(status);
        return drugRepository.findAll().stream()
                .filter(drug -> normalizedStatus == null || normalizedStatus.equalsIgnoreCase(drug.getStatus()))
                .filter(drug -> normalizedKeyword.isBlank()
                        || containsIgnoreCase(drug.getName(), normalizedKeyword)
                        || containsIgnoreCase(drug.getCode(), normalizedKeyword)
                        || containsIgnoreCase(drug.getPinyinCode(), normalizedKeyword))
                .sorted(Comparator.comparing(DrugEntity::getName, String.CASE_INSENSITIVE_ORDER))
                .map(this::toDrugSummary)
                .toList();
    }

    @Transactional
    public DrugSummary createDrug(ActorContext actorContext, DrugRequest request) {
        requireAdmin(actorContext);
        if (drugRepository.findByCode(request.code()).isPresent()) {
            throw conflict("drug code already exists");
        }
        DrugEntity entity = new DrugEntity();
        applyDrugRequest(entity, request);
        return toDrugSummary(drugRepository.save(entity));
    }

    @Transactional
    public DrugSummary updateDrug(ActorContext actorContext, Long id, DrugRequest request) {
        requireAdmin(actorContext);
        DrugEntity entity = requireDrug(id);
        if (!Objects.equals(entity.getCode(), request.code()) && drugRepository.findByCode(request.code()).isPresent()) {
            throw conflict("drug code already exists");
        }
        applyDrugRequest(entity, request);
        return toDrugSummary(drugRepository.save(entity));
    }

    @Transactional
    public DrugSummary toggleDrugStatus(ActorContext actorContext, Long id) {
        requireAdmin(actorContext);
        DrugEntity entity = requireDrug(id);
        entity.setStatus(isActive(entity.getStatus()) ? INACTIVE : ACTIVE);
        return toDrugSummary(drugRepository.save(entity));
    }

    @Transactional
    public List<PrescriptionRuleSummary> listRules(ActorContext actorContext, String ruleType, String status) {
        requireAdmin(actorContext);
        return ruleRepository.findAll().stream()
                .filter(rule -> ruleType == null || ruleType.isBlank() || ruleType.equalsIgnoreCase(rule.getRuleType()))
                .filter(rule -> status == null || status.isBlank() || status.equalsIgnoreCase(rule.getStatus()))
                .sorted(Comparator.comparing(PrescriptionRuleDefinitionEntity::getRuleCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::toRuleSummary)
                .toList();
    }

    @Transactional
    public PrescriptionRuleSummary createRule(ActorContext actorContext, PrescriptionRuleRequest request) {
        requireAdmin(actorContext);
        if (ruleRepository.findByRuleCode(request.ruleCode()).isPresent()) {
            throw conflict("rule code already exists");
        }
        PrescriptionRuleDefinitionEntity entity = new PrescriptionRuleDefinitionEntity();
        applyRuleRequest(entity, request);
        return toRuleSummary(ruleRepository.save(entity));
    }

    @Transactional
    public PrescriptionRuleSummary updateRule(ActorContext actorContext, Long id, PrescriptionRuleRequest request) {
        requireAdmin(actorContext);
        PrescriptionRuleDefinitionEntity entity = requireRule(id);
        if (!Objects.equals(entity.getRuleCode(), request.ruleCode()) && ruleRepository.findByRuleCode(request.ruleCode()).isPresent()) {
            throw conflict("rule code already exists");
        }
        applyRuleRequest(entity, request);
        return toRuleSummary(ruleRepository.save(entity));
    }

    @Transactional
    public PrescriptionRuleSummary toggleRuleStatus(ActorContext actorContext, Long id) {
        requireAdmin(actorContext);
        PrescriptionRuleDefinitionEntity entity = requireRule(id);
        entity.setStatus(isActive(entity.getStatus()) ? INACTIVE : ACTIVE);
        return toRuleSummary(ruleRepository.save(entity));
    }

    @Transactional
    public List<AiConfigSummary> listAiConfigs(ActorContext actorContext) {
        requireAdmin(actorContext);
        return aiConfigRepository.findAll().stream()
                .sorted(Comparator.comparing(AIConfigEntity::getTaskScope, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(AIConfigEntity::getPriority, Comparator.reverseOrder()))
                .map(this::toAiConfigSummary)
                .toList();
    }

    @Transactional
    public AiConfigSummary createAiConfig(ActorContext actorContext, AiConfigRequest request) {
        requireAdmin(actorContext);
        AIConfigEntity entity = new AIConfigEntity();
        applyAiConfigRequest(entity, request);
        return toAiConfigSummary(aiConfigRepository.save(entity));
    }

    @Transactional
    public AiConfigSummary updateAiConfig(ActorContext actorContext, Long id, AiConfigRequest request) {
        requireAdmin(actorContext);
        AIConfigEntity entity = requireAiConfig(id);
        applyAiConfigRequest(entity, request);
        return toAiConfigSummary(aiConfigRepository.save(entity));
    }

    @Transactional
    public AiConfigSummary toggleAiConfigStatus(ActorContext actorContext, Long id) {
        requireAdmin(actorContext);
        AIConfigEntity entity = requireAiConfig(id);
        entity.setStatus(isActive(entity.getStatus()) ? INACTIVE : ACTIVE);
        entity.setEnabled(!Boolean.TRUE.equals(entity.getEnabled()));
        return toAiConfigSummary(aiConfigRepository.save(entity));
    }

    @Transactional
    public AiConfigSummary rotateAiConfigKey(ActorContext actorContext, Long id, String apiKey, String keyVersion) {
        requireAdmin(actorContext);
        AIConfigEntity entity = requireAiConfig(id);
        entity.setApiKeyEncrypted(configCipher.encrypt(apiKey));
        entity.setKeyVersion(blankToNull(keyVersion));
        return toAiConfigSummary(aiConfigRepository.save(entity));
    }

    @Transactional
    public List<PromptTemplateSummary> listPromptTemplates(ActorContext actorContext) {
        requireAdmin(actorContext);
        return promptTemplateRepository.findAll().stream()
                .sorted(Comparator.comparing(PromptTemplateEntity::getTaskType, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PromptTemplateEntity::getVersion, Comparator.reverseOrder()))
                .map(this::toPromptTemplateSummary)
                .toList();
    }

    @Transactional
    public PromptTemplateSummary createPromptTemplate(ActorContext actorContext, PromptTemplateRequest request) {
        requireAdmin(actorContext);
        PromptTemplateEntity entity = new PromptTemplateEntity();
        applyPromptTemplateRequest(entity, request);
        return toPromptTemplateSummary(promptTemplateRepository.save(entity));
    }

    @Transactional
    public PromptTemplateSummary updatePromptTemplate(ActorContext actorContext, Long id, PromptTemplateRequest request) {
        requireAdmin(actorContext);
        PromptTemplateEntity entity = promptTemplateRepository.findById(id)
                .orElseThrow(() -> notFound("prompt template not found"));
        applyPromptTemplateRequest(entity, request);
        return toPromptTemplateSummary(promptTemplateRepository.save(entity));
    }

    @Transactional
    public PromptTemplateSummary togglePromptTemplateStatus(ActorContext actorContext, Long id) {
        requireAdmin(actorContext);
        PromptTemplateEntity entity = promptTemplateRepository.findById(id)
                .orElseThrow(() -> notFound("prompt template not found"));
        entity.setStatus(isActive(entity.getStatus()) ? INACTIVE : ACTIVE);
        return toPromptTemplateSummary(promptTemplateRepository.save(entity));
    }

    private void applyDrugRequest(DrugEntity entity, DrugRequest request) {
        entity.setCode(request.code().trim());
        entity.setName(request.name().trim());
        entity.setPinyinCode(blankToNull(request.pinyinCode()));
        entity.setSpecification(blankToNull(request.specification()));
        entity.setDosageForm(blankToNull(request.dosageForm()));
        entity.setPackageUnit(blankToNull(request.packageUnit()));
        entity.setManufacturer(blankToNull(request.manufacturer()));
        entity.setUnitPrice(request.unitPrice());
        entity.setDefaultUsage(blankToNull(request.defaultUsage()));
        entity.setContraindications(blankToNull(request.contraindications()));
        entity.setPrecautions(blankToNull(request.precautions()));
        entity.setIndications(blankToNull(request.indications()));
        entity.setInteractionSummary(blankToNull(request.interactionSummary()));
        entity.setStatus(normalizeStatus(request.status()));
    }

    private void applyRuleRequest(PrescriptionRuleDefinitionEntity entity, PrescriptionRuleRequest request) {
        entity.setRuleCode(request.ruleCode().trim());
        entity.setRuleType(request.ruleType().trim());
        entity.setApplicableDrugs(blankToNull(request.applicableDrugs()));
        entity.setApplicableDiseases(blankToNull(request.applicableDiseases()));
        entity.setApplicablePopulations(blankToNull(request.applicablePopulations()));
        entity.setConditionExpression(blankToNull(request.conditionExpression()));
        entity.setRiskLevel(blankToNull(request.riskLevel()));
        entity.setAlertMessage(blankToNull(request.alertMessage()));
        entity.setSuggestion(blankToNull(request.suggestion()));
        entity.setBasis(blankToNull(request.basis()));
        entity.setSeeded(request.seeded() != null && request.seeded());
        entity.setValidationStatus(blankToNull(request.validationStatus()));
        entity.setStatus(normalizeStatus(request.status()));
        if (entity.getVersion() == null) {
            entity.setVersion(0);
        }
    }

    private void applyAiConfigRequest(AIConfigEntity entity, AiConfigRequest request) {
        entity.setProvider(request.provider().trim());
        entity.setModelName(request.modelName().trim());
        entity.setApiUrl(blankToNull(request.apiUrl()));
        entity.setTaskScope(request.taskScope().trim());
        entity.setTimeoutSeconds(request.timeoutSeconds());
        entity.setDefaultConfig(request.defaultConfig() != null && request.defaultConfig());
        entity.setHealthStatus(blankToNull(request.healthStatus()));
        entity.setConfigVersion(firstNonBlank(request.configVersion(), "v1"));
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setPriority(request.priority());
        entity.setStatus(normalizeStatus(request.status()));
        if (request.apiKey() != null && !request.apiKey().isBlank()) {
            entity.setApiKeyEncrypted(configCipher.encrypt(request.apiKey()));
            entity.setKeyVersion(firstNonBlank(request.keyVersion(), "current"));
        }
        if (entity.getConfigVersion() == null) {
            entity.setConfigVersion("v1");
        }
    }

    private void applyPromptTemplateRequest(PromptTemplateEntity entity, PromptTemplateRequest request) {
        entity.setTemplateCode(request.templateCode().trim());
        entity.setTaskType(request.taskType().trim());
        entity.setDeptCode(blankToNull(request.deptCode()));
        entity.setTemplateBody(blankToNull(request.templateBody()));
        entity.setVariableWhitelist(blankToNull(request.variableWhitelist()));
        entity.setVersion(request.version() == null ? 0 : request.version());
        entity.setDefaultTemplate(request.defaultTemplate() != null && request.defaultTemplate());
        entity.setStatus(normalizeStatus(request.status()));
    }

    private ScheduleEntity saveSchedule(ScheduleEntity entity, ScheduleRequest request) {
        DoctorEntity doctor = requireDoctor(request.doctorId());
        DepartmentEntity department = requireDepartment(request.departmentId());
        String normalizedStatus = normalizeStatus(request.status());
        if (ACTIVE.equals(normalizedStatus) && (!ACTIVE.equals(doctor.getStatus()) || !ACTIVE.equals(department.getStatus()))) {
            throw conflict("active schedule requires active doctor and department");
        }
        Integer totalSlots = request.totalSlots();
        Integer remainingSlots = request.remainingSlots() == null ? totalSlots : request.remainingSlots();
        if (remainingSlots > totalSlots) {
            throw conflict("remaining slots cannot exceed total slots");
        }
        entity.setDoctorId(doctor.getId());
        entity.setDepartmentId(request.departmentId());
        entity.setWorkDate(request.workDate());
        entity.setPeriod(request.period().trim());
        entity.setTotalSlots(totalSlots);
        entity.setRemainingSlots(remainingSlots);
        entity.setVisitLevel(request.visitLevel().trim());
        entity.setStatus(normalizedStatus);
        return scheduleRepository.save(entity);
    }

    private DepartmentSummary toDepartmentSummary(DepartmentEntity entity) {
        long doctorCount = doctorRepository.findAll().stream()
                .filter(doctor -> Objects.equals(doctor.getDepartmentId(), entity.getId()))
                .count();
        long activeScheduleCount = scheduleRepository.findAll().stream()
                .filter(schedule -> Objects.equals(schedule.getDepartmentId(), entity.getId()))
                .filter(schedule -> ACTIVE.equals(schedule.getStatus()))
                .count();
        return new DepartmentSummary(entity.getId(), entity.getCode(), entity.getName(), entity.getType(), entity.getDescription(), entity.getStatus(), doctorCount, activeScheduleCount, entity.getUpdatedAt());
    }

    private DoctorSummary toDoctorSummary(DoctorEntity entity) {
        String departmentName = departmentRepository.findById(entity.getDepartmentId() == null ? -1L : entity.getDepartmentId())
                .map(DepartmentEntity::getName)
                .orElse(null);
        long scheduleCount = scheduleRepository.findAll().stream()
                .filter(schedule -> Objects.equals(schedule.getDoctorId(), entity.getId()))
                .count();
        return new DoctorSummary(entity.getId(), entity.getUsername(), entity.getName(), entity.getDepartmentId(), departmentName, entity.getTitle(), entity.getSpecialty(), entity.getIntroduction(), entity.getStatus(), scheduleCount, entity.getUpdatedAt());
    }

    private ScheduleSummary toScheduleSummary(ScheduleEntity entity) {
        DoctorEntity doctor = doctorRepository.findById(entity.getDoctorId()).orElse(null);
        DepartmentEntity department = departmentRepository.findById(entity.getDepartmentId()).orElse(null);
        return new ScheduleSummary(
                entity.getId(),
                entity.getDoctorId(),
                doctor == null ? null : doctor.getName(),
                entity.getDepartmentId(),
                department == null ? null : department.getName(),
                entity.getWorkDate(),
                entity.getPeriod(),
                entity.getTotalSlots(),
                entity.getRemainingSlots(),
                entity.getVisitLevel(),
                entity.getStatus(),
                entity.getUpdatedAt()
        );
    }

    private DrugSummary toDrugSummary(DrugEntity entity) {
        return new DrugSummary(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getPinyinCode(),
                entity.getSpecification(),
                entity.getDosageForm(),
                entity.getPackageUnit(),
                entity.getManufacturer(),
                entity.getUnitPrice(),
                entity.getDefaultUsage(),
                entity.getContraindications(),
                entity.getPrecautions(),
                entity.getIndications(),
                entity.getInteractionSummary(),
                entity.getStatus(),
                entity.getUpdatedAt()
        );
    }

    private PrescriptionRuleSummary toRuleSummary(PrescriptionRuleDefinitionEntity entity) {
        return new PrescriptionRuleSummary(
                entity.getId(),
                entity.getRuleCode(),
                entity.getRuleType(),
                entity.getApplicableDrugs(),
                entity.getApplicableDiseases(),
                entity.getApplicablePopulations(),
                entity.getConditionExpression(),
                entity.getRiskLevel(),
                entity.getAlertMessage(),
                entity.getSuggestion(),
                entity.getBasis(),
                entity.getSeeded(),
                entity.getVersion(),
                entity.getValidationStatus(),
                entity.getStatus(),
                entity.getUpdatedAt()
        );
    }

    private AiConfigSummary toAiConfigSummary(AIConfigEntity entity) {
        return new AiConfigSummary(
                entity.getId(),
                entity.getProvider(),
                entity.getModelName(),
                entity.getApiUrl(),
                entity.getTaskScope(),
                entity.getTimeoutSeconds(),
                entity.getDefaultConfig(),
                entity.getHealthStatus(),
                entity.getConfigVersion(),
                entity.getEnabled(),
                entity.getPriority(),
                entity.getStatus(),
                entity.getApiKeyEncrypted() != null && !entity.getApiKeyEncrypted().isBlank(),
                entity.getKeyVersion(),
                entity.getUpdatedAt()
        );
    }

    private PromptTemplateSummary toPromptTemplateSummary(PromptTemplateEntity entity) {
        return new PromptTemplateSummary(
                entity.getId(),
                entity.getTemplateCode(),
                entity.getTaskType(),
                entity.getDeptCode(),
                entity.getTemplateBody(),
                entity.getVariableWhitelist(),
                entity.getVersion(),
                entity.getDefaultTemplate(),
                entity.getStatus(),
                entity.getUpdatedAt()
        );
    }

    private DepartmentEntity requireDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> notFound("department not found"));
    }

    private DoctorEntity requireDoctor(Long id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> notFound("doctor not found"));
    }

    private ScheduleEntity requireSchedule(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> notFound("schedule not found"));
    }

    private DrugEntity requireDrug(Long id) {
        return drugRepository.findById(id)
                .orElseThrow(() -> notFound("drug not found"));
    }

    private PrescriptionRuleDefinitionEntity requireRule(Long id) {
        return ruleRepository.findById(id)
                .orElseThrow(() -> notFound("rule not found"));
    }

    private AIConfigEntity requireAiConfig(Long id) {
        return aiConfigRepository.findById(id)
                .orElseThrow(() -> notFound("ai config not found"));
    }

    private void requireAdmin(ActorContext actorContext) {
        if (actorContext == null || !rolePolicy.canViewDashboardAsAdmin(actorContext)) {
            throw forbidden("admin permission required");
        }
    }

    private boolean isActive(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        return ACTIVE.equals(normalized);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ACTIVE;
        }
        return status.trim().toUpperCase(Locale.ROOT);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private boolean containsIgnoreCase(String source, String keyword) {
        return source != null && keyword != null && source.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT.value(), message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND.value(), message);
    }

    private ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN.value(), message);
    }
}
