package com.cloudbrain.application.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.cloudbrain.entity.core.PromptTemplateEntity;
import com.cloudbrain.repository.PromptTemplateJpaRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptTemplateServiceTest {

    private PromptTemplateJpaRepository repository;
    private PromptTemplateService service;

    @BeforeEach
    void setUp() {
        repository = org.mockito.Mockito.mock(PromptTemplateJpaRepository.class);
        service = new PromptTemplateService(repository);
    }

    @Test
    void resolveUsesDepartmentTemplateAndHonorsWhitelist() {
        PromptTemplateEntity template = template("triage-cardio", "TRIAGE", "cardiology",
                "主诉={{chiefComplaint}}; 科室={{departmentName}}; 泄漏={{secret}}",
                "[\"chiefComplaint\", \"departmentName\"]", 3);
        when(repository.findFirstByTaskTypeAndDeptCodeAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
                "TRIAGE", "cardiology", "ACTIVE")).thenReturn(Optional.of(template));

        var resolved = service.resolve(" triage ", " cardiology ",
                Map.of("chiefComplaint", "胸痛", "departmentName", "心内科", "secret", "hidden"));

        assertThat(resolved.templateCode()).isEqualTo("triage-cardio");
        assertThat(resolved.promptVersion()).isEqualTo("triage-cardio-v3");
        assertThat(resolved.body()).contains("主诉=胸痛", "科室=心内科", "泄漏=");
        assertThat(resolved.body()).doesNotContain("hidden");
    }

    @Test
    void resolveFallsBackFromDepartmentToGlobalThenAnyActiveTemplate() {
        PromptTemplateEntity global = template("medical-global", "MEDICAL_RECORD", null,
                "问诊={{conversationText}}", null, 2);
        when(repository.findFirstByTaskTypeAndDeptCodeAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
                "MEDICAL_RECORD", "cardiology", "ACTIVE")).thenReturn(Optional.empty());
        when(repository.findFirstByTaskTypeAndDeptCodeIsNullAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
                "MEDICAL_RECORD", "ACTIVE")).thenReturn(Optional.of(global));

        var resolved = service.resolve("medical_record", "cardiology", Map.of("conversationText", "咳嗽"));

        assertThat(resolved.templateCode()).isEqualTo("medical-global");
        assertThat(resolved.body()).isEqualTo("问诊=咳嗽");
    }

    @Test
    void resolveUsesBuiltinWhenRepositoryHasNoTemplateOrTemplateRendersBlank() {
        when(repository.findFirstByTaskTypeAndDeptCodeIsNullAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
                "DIAGNOSIS", "ACTIVE")).thenReturn(Optional.empty());
        when(repository.findByTaskTypeAndStatusOrderByVersionDesc("DIAGNOSIS", "ACTIVE")).thenReturn(List.of());

        var builtin = service.resolve("diagnosis", null, Map.of("conversationText", "头痛"));
        assertThat(builtin.templateCode()).isEqualTo("builtin-diagnosis");
        assertThat(builtin.version()).isZero();
        assertThat(builtin.body()).isNotBlank();

        PromptTemplateEntity blank = template("blank", "TRIAGE", null, "   ", null, 5);
        when(repository.findFirstByTaskTypeAndDeptCodeIsNullAndDefaultTemplateTrueAndStatusOrderByVersionDesc(
                "TRIAGE", "ACTIVE")).thenReturn(Optional.of(blank));
        var rendered = service.resolve("TRIAGE", null, Map.of("chiefComplaint", "腹痛"));
        assertThat(rendered.templateCode()).isEqualTo("blank");
        assertThat(rendered.body()).isNotBlank();
    }

    private PromptTemplateEntity template(String code, String taskType, String deptCode, String body, String whitelist, int version) {
        PromptTemplateEntity entity = new PromptTemplateEntity();
        entity.setTemplateCode(code);
        entity.setTaskType(taskType);
        entity.setDeptCode(deptCode);
        entity.setTemplateBody(body);
        entity.setVariableWhitelist(whitelist);
        entity.setVersion(version);
        entity.setDefaultTemplate(true);
        entity.setStatus("ACTIVE");
        return entity;
    }
}
