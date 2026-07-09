package com.cloudbrain.application.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.dto.admin.AdminDtos.AiConfigRequest;
import com.cloudbrain.dto.admin.AdminDtos.BatchScheduleRequest;
import com.cloudbrain.dto.admin.AdminDtos.DepartmentRequest;
import com.cloudbrain.dto.admin.AdminDtos.DoctorRequest;
import com.cloudbrain.dto.admin.AdminDtos.DrugRequest;
import com.cloudbrain.dto.admin.AdminDtos.PromptTemplateRequest;
import com.cloudbrain.dto.admin.AdminDtos.PrescriptionRuleRequest;
import com.cloudbrain.dto.admin.AdminDtos.ScheduleRequest;
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
import com.cloudbrain.security.ActorRole;
import com.cloudbrain.security.DefaultRolePolicy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminServiceTest {

    private DepartmentJpaRepository departments;
    private DoctorJpaRepository doctors;
    private ScheduleJpaRepository schedules;
    private DrugJpaRepository drugs;
    private PrescriptionRuleDefinitionJpaRepository rules;
    private AIConfigJpaRepository aiConfigs;
    private PromptTemplateJpaRepository promptTemplates;
    private PasswordEncoder passwordEncoder;
    private ConfigCipher cipher;
    private AdminService service;

    @BeforeEach
    void setUp() {
        departments = org.mockito.Mockito.mock(DepartmentJpaRepository.class);
        doctors = org.mockito.Mockito.mock(DoctorJpaRepository.class);
        schedules = org.mockito.Mockito.mock(ScheduleJpaRepository.class);
        drugs = org.mockito.Mockito.mock(DrugJpaRepository.class);
        rules = org.mockito.Mockito.mock(PrescriptionRuleDefinitionJpaRepository.class);
        aiConfigs = org.mockito.Mockito.mock(AIConfigJpaRepository.class);
        promptTemplates = org.mockito.Mockito.mock(PromptTemplateJpaRepository.class);
        passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        cipher = new ConfigCipher("unit-test-secret-that-is-long-enough");
        service = new AdminService(
                departments,
                doctors,
                schedules,
                drugs,
                rules,
                aiConfigs,
                promptTemplates,
                org.mockito.Mockito.mock(AuditLogJpaRepository.class),
                passwordEncoder,
                cipher,
                new DefaultRolePolicy()
        );
    }

    @Test
    void departmentCrudAndPermissionChecksCoverAdminResourceCore() {
        DepartmentEntity cardiology = department(1L, "cardiology", "Cardiology", "ACTIVE");
        DepartmentEntity neuro = department(2L, "neurology", "Neurology", "INACTIVE");
        when(departments.findAll()).thenReturn(List.of(neuro, cardiology));
        when(doctors.findAll()).thenReturn(List.of(doctor(11L, "doc1", "Dr Zhang", 1L, "ACTIVE")));
        when(schedules.findAll()).thenReturn(List.of(schedule(21L, 11L, 1L, "ACTIVE", 10, 8)));

        assertThat(service.listDepartments(admin())).extracting("name").containsExactly("Cardiology", "Neurology");
        assertThat(service.listDepartments(admin()).get(0).doctorCount()).isEqualTo(1);
        assertThat(service.listDepartments(admin()).get(0).activeScheduleCount()).isEqualTo(1);

        when(departments.findByCode("new")).thenReturn(Optional.empty());
        when(departments.save(any(DepartmentEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 3L));
        var created = service.createDepartment(admin(), new DepartmentRequest(" new ", " New Department ", "clinic", " description ", ""));
        assertThat(created.code()).isEqualTo("new");
        assertThat(created.status()).isEqualTo("ACTIVE");

        when(departments.findById(1L)).thenReturn(Optional.of(cardiology));
        var toggled = service.toggleDepartmentStatus(admin(), 1L);
        assertThat(toggled.status()).isEqualTo("INACTIVE");

        assertThatThrownBy(() -> service.listDepartments(patient()))
                .isInstanceOf(ApiException.class)
                .hasMessage("admin permission required");
    }

    @Test
    void doctorAndScheduleOperationsValidateDuplicatesCapacityAndStatus() {
        DepartmentEntity activeDept = department(1L, "cardiology", "Cardiology", "ACTIVE");
        DoctorEntity activeDoctor = doctor(11L, "doc1", "Dr Zhang", 1L, "ACTIVE");
        when(departments.findById(1L)).thenReturn(Optional.of(activeDept));
        when(doctors.findById(11L)).thenReturn(Optional.of(activeDoctor));
        when(doctors.existsByUsername("doc1")).thenReturn(false);
        when(passwordEncoder.encode("doctor123")).thenReturn("{bcrypt}default");
        when(doctors.save(any(DoctorEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 12L));

        var doctor = service.createDoctor(admin(), new DoctorRequest("doc1", "", "Dr Zhang", 1L, "Chief", "Cardiology", "intro", "active"));

        assertThat(doctor.username()).isEqualTo("doc1");
        assertThat(doctor.status()).isEqualTo("ACTIVE");
        verify(passwordEncoder).encode("doctor123");

        when(schedules.save(any(ScheduleEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 31L));
        var schedule = service.createSchedule(admin(),
                new ScheduleRequest(11L, 1L, LocalDate.now().plusDays(1), "AM", 10, null, "normal", "ACTIVE"));
        assertThat(schedule.remainingSlots()).isEqualTo(10);

        assertThatThrownBy(() -> service.createSchedule(admin(),
                new ScheduleRequest(11L, 1L, LocalDate.now(), "PM", 3, 4, "normal", "ACTIVE")))
                .isInstanceOf(ApiException.class)
                .hasMessage("remaining slots cannot exceed total slots");

        activeDept.setStatus("INACTIVE");
        assertThatThrownBy(() -> service.createSchedule(admin(),
                new ScheduleRequest(11L, 1L, LocalDate.now(), "Night", 3, 1, "normal", "ACTIVE")))
                .isInstanceOf(ApiException.class)
                .hasMessage("active schedule requires active doctor and department");
    }

    @Test
    void drugRuleAiConfigAndPromptTemplateOperationsMapFieldsAndSecrets() {
        when(drugs.findByCode("D001")).thenReturn(Optional.empty());
        when(drugs.save(any(DrugEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 41L));
        var drug = service.createDrug(admin(), new DrugRequest("D001", "Aspirin", "ASP", "100mg", "tablet",
                "box", "maker", new BigDecimal("12.50"), "oral", "allergy", "caution", "fever", "interaction", "ACTIVE"));
        assertThat(drug.name()).isEqualTo("Aspirin");
        assertThat(drug.unitPrice()).isEqualByComparingTo("12.50");

        when(rules.findByRuleCode("R001")).thenReturn(Optional.empty());
        when(rules.save(any(PrescriptionRuleDefinitionEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 51L));
        var rule = service.createRule(admin(), new PrescriptionRuleRequest("R001", "DOSAGE", "Aspirin", "ulcer",
                "elderly", "quantity>10", "HIGH", "alert", "suggestion", "basis", true, "VALID", ""));
        assertThat(rule.status()).isEqualTo("ACTIVE");
        assertThat(rule.version()).isZero();

        when(aiConfigs.save(any(AIConfigEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 61L));
        var config = service.createAiConfig(admin(), new AiConfigRequest("deepseek", "chat", "https://api", "secret",
                null, "TRIAGE", 30, true, "OK", "", null, 5, null));
        assertThat(config.status()).isEqualTo("ACTIVE");
        assertThat(config.enabled()).isTrue();
        assertThat(config.hasApiKey()).isTrue();
        assertThat(config.configVersion()).isEqualTo("v1");

        AIConfigEntity stored = new AIConfigEntity();
        stored.setId(61L);
        when(aiConfigs.findById(61L)).thenReturn(Optional.of(stored));
        when(aiConfigs.save(stored)).thenReturn(stored);
        service.rotateAiConfigKey(admin(), 61L, "rotated", "kv2");
        assertThat(cipher.decrypt(stored.getApiKeyEncrypted())).isEqualTo("rotated");
        assertThat(stored.getKeyVersion()).isEqualTo("kv2");

        when(promptTemplates.save(any(PromptTemplateEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 71L));
        var template = service.createPromptTemplate(admin(),
                new PromptTemplateRequest("tpl", "TRIAGE", "cardiology", "body", "chiefComplaint", null, true, ""));
        assertThat(template.version()).isZero();
        assertThat(template.status()).isEqualTo("ACTIVE");
    }

    @Test
    void adminResourceUpdateListToggleAndConflictPathsCoverCrudCore() {
        DepartmentEntity cardiology = department(1L, "cardiology", "Cardiology", "ACTIVE");
        DepartmentEntity neurology = department(2L, "neurology", "Neurology", "ACTIVE");
        DoctorEntity zhang = doctor(11L, "zhang", "Dr Zhang", 1L, "ACTIVE");
        DoctorEntity li = doctor(12L, "li", "Dr Li", 2L, "INACTIVE");
        ScheduleEntity morning = schedule(21L, 11L, 1L, "ACTIVE", 10, 5);
        ScheduleEntity afternoon = schedule(22L, 12L, 2L, "INACTIVE", 8, 8);
        DrugEntity aspirin = drug(31L, "D001", "Aspirin", "ASP", "ACTIVE");
        DrugEntity ibuprofen = drug(32L, "D002", "Ibuprofen", "IBU", "INACTIVE");
        PrescriptionRuleDefinitionEntity rule = rule(41L, "R001", "DOSAGE", "ACTIVE");
        AIConfigEntity aiConfig = aiConfig(51L, "TRIAGE", 10, "ACTIVE", true);
        PromptTemplateEntity template = promptTemplate(61L, "TPL", "TRIAGE", 1, "ACTIVE");

        when(departments.findById(1L)).thenReturn(Optional.of(cardiology));
        when(departments.findById(2L)).thenReturn(Optional.of(neurology));
        when(departments.findByCode("new-code")).thenReturn(Optional.empty());
        when(departments.findByCode("neurology")).thenReturn(Optional.of(neurology));
        when(departments.save(any(DepartmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updatedDepartment = service.updateDepartment(admin(), 1L,
                new DepartmentRequest("new-code", "New Cardiology", "clinic", "new description", "inactive"));
        assertThat(updatedDepartment.code()).isEqualTo("new-code");
        assertThat(updatedDepartment.status()).isEqualTo("INACTIVE");
        assertThatThrownBy(() -> service.updateDepartment(admin(), 1L,
                new DepartmentRequest("neurology", "Duplicate", "clinic", "", "ACTIVE")))
                .isInstanceOf(ApiException.class)
                .hasMessage("department code already exists");

        when(doctors.findAll()).thenReturn(List.of(li, zhang));
        when(schedules.findAll()).thenReturn(List.of(afternoon, morning));
        when(departments.findById(zhang.getDepartmentId())).thenReturn(Optional.of(cardiology));
        when(departments.findById(li.getDepartmentId())).thenReturn(Optional.of(neurology));
        when(doctors.findById(11L)).thenReturn(Optional.of(zhang));
        when(doctors.findById(12L)).thenReturn(Optional.of(li));
        when(doctors.existsByUsername("newdoc")).thenReturn(false);
        when(doctors.existsByUsername("li")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("{bcrypt}newpass");
        when(doctors.save(any(DoctorEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.listDoctors(admin(), 1L)).extracting("username").containsExactly("zhang");
        var updatedDoctor = service.updateDoctor(admin(), 11L,
                new DoctorRequest("newdoc", "newpass", "Dr New", 2L, "Attending", "Neuro", "intro", "ACTIVE"));
        assertThat(updatedDoctor.username()).isEqualTo("newdoc");
        assertThat(updatedDoctor.departmentName()).isEqualTo("Neurology");
        verify(passwordEncoder).encode("newpass");
        assertThat(service.toggleDoctorStatus(admin(), 12L).status()).isEqualTo("ACTIVE");
        assertThatThrownBy(() -> service.updateDoctor(admin(), 11L,
                new DoctorRequest("li", "", "Dr Duplicate", 2L, null, null, null, "ACTIVE")))
                .isInstanceOf(ApiException.class)
                .hasMessage("doctor username already exists");

        when(schedules.findById(21L)).thenReturn(Optional.of(morning));
        when(schedules.save(any(ScheduleEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        cardiology.setStatus("ACTIVE");
        assertThat(service.listSchedules(admin(), 11L, 1L, LocalDate.now(), LocalDate.now().plusDays(2))).hasSize(1);
        assertThat(service.updateSchedule(admin(), 21L,
                new ScheduleRequest(11L, 1L, LocalDate.now().plusDays(1), "PM", 12, 6, "expert", "ACTIVE")).period())
                .isEqualTo("PM");
        assertThat(service.toggleScheduleStatus(admin(), 21L).status()).isEqualTo("INACTIVE");

        when(drugs.findAll()).thenReturn(List.of(ibuprofen, aspirin));
        when(drugs.findById(31L)).thenReturn(Optional.of(aspirin));
        when(drugs.findByCode("D010")).thenReturn(Optional.empty());
        when(drugs.findByCode("D002")).thenReturn(Optional.of(ibuprofen));
        when(drugs.save(any(DrugEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.listDrugs(admin(), "asp", "ACTIVE")).extracting("code").containsExactly("D001");
        assertThat(service.updateDrug(admin(), 31L, new DrugRequest("D010", "Aspirin Plus", "ASP", "200mg",
                "tablet", "box", "maker", new BigDecimal("20.00"), "oral", "", "", "pain", "", "ACTIVE")).code())
                .isEqualTo("D010");
        assertThat(service.toggleDrugStatus(admin(), 31L).status()).isEqualTo("INACTIVE");
        assertThatThrownBy(() -> service.updateDrug(admin(), 31L, new DrugRequest("D002", "Duplicate", null, null,
                null, null, null, null, null, null, null, null, null, "ACTIVE")))
                .isInstanceOf(ApiException.class)
                .hasMessage("drug code already exists");

        when(rules.findAll()).thenReturn(List.of(rule));
        when(rules.findById(41L)).thenReturn(Optional.of(rule));
        when(rules.findByRuleCode("R010")).thenReturn(Optional.empty());
        when(rules.findByRuleCode("R001")).thenReturn(Optional.of(rule));
        when(rules.save(any(PrescriptionRuleDefinitionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.listRules(admin(), "DOSAGE", "ACTIVE")).hasSize(1);
        assertThat(service.updateRule(admin(), 41L, new PrescriptionRuleRequest("R010", "ALLERGY",
                "Aspirin", "", "", "contains", "HIGH", "alert", "stop", "basis", false, "VALID", "ACTIVE")).ruleCode())
                .isEqualTo("R010");
        assertThat(service.toggleRuleStatus(admin(), 41L).status()).isEqualTo("INACTIVE");
        assertThatThrownBy(() -> service.updateRule(admin(), 41L, new PrescriptionRuleRequest("R001", "ALLERGY",
                "", "", "", "", "LOW", "", "", "", false, "VALID", "ACTIVE")))
                .isInstanceOf(ApiException.class)
                .hasMessage("rule code already exists");

        when(aiConfigs.findAll()).thenReturn(List.of(aiConfig));
        when(aiConfigs.findById(51L)).thenReturn(Optional.of(aiConfig));
        when(aiConfigs.save(any(AIConfigEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.listAiConfigs(admin())).hasSize(1);
        assertThat(service.updateAiConfig(admin(), 51L, new AiConfigRequest("deepseek", "model2", "", "",
                "", "CHAT", 45, false, "OK", "ACTIVE", true, 3, "v2")).modelName()).isEqualTo("model2");
        var toggledConfig = service.toggleAiConfigStatus(admin(), 51L);
        assertThat(toggledConfig.status()).isEqualTo("INACTIVE");
        assertThat(toggledConfig.enabled()).isFalse();

        when(promptTemplates.findAll()).thenReturn(List.of(template));
        when(promptTemplates.findById(61L)).thenReturn(Optional.of(template));
        when(promptTemplates.save(any(PromptTemplateEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.listPromptTemplates(admin())).hasSize(1);
        assertThat(service.updatePromptTemplate(admin(), 61L,
                new PromptTemplateRequest("TPL2", "CHAT", "", "body2", "", 2, false, "ACTIVE")).templateCode())
                .isEqualTo("TPL2");
        assertThat(service.togglePromptTemplateStatus(admin(), 61L).status()).isEqualTo("INACTIVE");
    }

    @Test
    void batchScheduleAndNotFoundPathsCoverBoundaryResourceManagement() {
        DepartmentEntity activeDept = department(1L, "cardiology", "Cardiology", "ACTIVE");
        DoctorEntity activeDoctor = doctor(11L, "doc1", "Dr Zhang", 1L, "ACTIVE");
        when(departments.findById(1L)).thenReturn(Optional.of(activeDept));
        when(doctors.findById(11L)).thenReturn(Optional.of(activeDoctor));
        when(schedules.save(any(ScheduleEntity.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 80L));

        var batch = service.batchCreateSchedules(admin(), new BatchScheduleRequest(
                11L,
                1L,
                List.of(LocalDate.now().plusDays(1), LocalDate.now().plusDays(2)),
                List.of("AM", "PM"),
                10,
                7,
                "normal",
                "ACTIVE"
        ));
        assertThat(batch).hasSize(4);
        assertThat(batch).allSatisfy(summary -> assertThat(summary.remainingSlots()).isEqualTo(7));

        when(departments.findById(404L)).thenReturn(Optional.empty());
        when(doctors.findById(404L)).thenReturn(Optional.empty());
        when(schedules.findById(404L)).thenReturn(Optional.empty());
        when(drugs.findById(404L)).thenReturn(Optional.empty());
        when(rules.findById(404L)).thenReturn(Optional.empty());
        when(aiConfigs.findById(404L)).thenReturn(Optional.empty());
        when(promptTemplates.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateDepartment(admin(), 404L,
                new DepartmentRequest("none", "None", "", "", "ACTIVE"))).hasMessage("department not found");
        assertThatThrownBy(() -> service.updateDoctor(admin(), 404L,
                new DoctorRequest("none", "", "None", 1L, "", "", "", "ACTIVE"))).hasMessage("doctor not found");
        assertThatThrownBy(() -> service.updateSchedule(admin(), 404L,
                new ScheduleRequest(11L, 1L, LocalDate.now(), "AM", 1, 1, "normal", "ACTIVE"))).hasMessage("schedule not found");
        assertThatThrownBy(() -> service.updateDrug(admin(), 404L,
                new DrugRequest("none", "None", null, null, null, null, null, null, null, null, null, null, null, "ACTIVE")))
                .hasMessage("drug not found");
        assertThatThrownBy(() -> service.updateRule(admin(), 404L,
                new PrescriptionRuleRequest("none", "TYPE", null, null, null, null, null, null, null, null, false, null, "ACTIVE")))
                .hasMessage("rule not found");
        assertThatThrownBy(() -> service.updateAiConfig(admin(), 404L,
                new AiConfigRequest("p", "m", null, null, null, "CHAT", 1, false, null, "ACTIVE", true, 1, null)))
                .hasMessage("ai config not found");
        assertThatThrownBy(() -> service.updatePromptTemplate(admin(), 404L,
                new PromptTemplateRequest("tpl", "CHAT", null, null, null, null, false, "ACTIVE")))
                .hasMessage("prompt template not found");
    }

    private ActorContext admin() {
        return new ActorContext(1L, ActorRole.ADMIN, null, null, "admin", "Admin");
    }

    private ActorContext patient() {
        return new ActorContext(2L, ActorRole.PATIENT, 100L, null, "patient", "Patient");
    }

    private DepartmentEntity department(Long id, String code, String name, String status) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setType("clinic");
        entity.setDescription("description");
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
        entity.setTitle("Chief");
        entity.setSpecialty("specialty");
        entity.setIntroduction("intro");
        entity.setStatus(status);
        return entity;
    }

    private ScheduleEntity schedule(Long id, Long doctorId, Long departmentId, String status, int total, int remaining) {
        ScheduleEntity entity = new ScheduleEntity();
        entity.setId(id);
        entity.setDoctorId(doctorId);
        entity.setDepartmentId(departmentId);
        entity.setWorkDate(LocalDate.now().plusDays(1));
        entity.setPeriod("AM");
        entity.setTotalSlots(total);
        entity.setRemainingSlots(remaining);
        entity.setVisitLevel("normal");
        entity.setStatus(status);
        entity.setVersion(0);
        return entity;
    }

    private DrugEntity drug(Long id, String code, String name, String pinyinCode, String status) {
        DrugEntity entity = new DrugEntity();
        entity.setId(id);
        entity.setCode(code);
        entity.setName(name);
        entity.setPinyinCode(pinyinCode);
        entity.setSpecification("100mg");
        entity.setDosageForm("tablet");
        entity.setPackageUnit("box");
        entity.setManufacturer("maker");
        entity.setUnitPrice(new BigDecimal("12.50"));
        entity.setDefaultUsage("oral");
        entity.setContraindications("none");
        entity.setPrecautions("caution");
        entity.setIndications("pain");
        entity.setInteractionSummary("none");
        entity.setStatus(status);
        return entity;
    }

    private PrescriptionRuleDefinitionEntity rule(Long id, String code, String type, String status) {
        PrescriptionRuleDefinitionEntity entity = new PrescriptionRuleDefinitionEntity();
        entity.setId(id);
        entity.setRuleCode(code);
        entity.setRuleType(type);
        entity.setApplicableDrugs("Aspirin");
        entity.setApplicableDiseases("cold");
        entity.setApplicablePopulations("adult");
        entity.setConditionExpression("contains");
        entity.setRiskLevel("LOW");
        entity.setAlertMessage("alert");
        entity.setSuggestion("suggest");
        entity.setBasis("basis");
        entity.setSeeded(false);
        entity.setValidationStatus("VALID");
        entity.setVersion(0);
        entity.setStatus(status);
        return entity;
    }

    private AIConfigEntity aiConfig(Long id, String taskScope, int priority, String status, boolean enabled) {
        AIConfigEntity entity = new AIConfigEntity();
        entity.setId(id);
        entity.setProvider("deepseek");
        entity.setModelName("model");
        entity.setApiUrl("https://api");
        entity.setTaskScope(taskScope);
        entity.setTimeoutSeconds(30);
        entity.setDefaultConfig(false);
        entity.setHealthStatus("OK");
        entity.setConfigVersion("v1");
        entity.setEnabled(enabled);
        entity.setPriority(priority);
        entity.setStatus(status);
        return entity;
    }

    private PromptTemplateEntity promptTemplate(Long id, String code, String taskType, int version, String status) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setId(id);
        entity.setTemplateCode(code);
        entity.setTaskType(taskType);
        entity.setDeptCode("cardiology");
        entity.setTemplateBody("body");
        entity.setVariableWhitelist("chiefComplaint");
        entity.setVersion(version);
        entity.setDefaultTemplate(true);
        entity.setStatus(status);
        return entity;
    }

    private <T extends com.cloudbrain.entity.BaseAuditableEntity> T withId(T entity, Long id) {
        if (entity.getId() == null) {
            entity.setId(id);
        }
        return entity;
    }
}
