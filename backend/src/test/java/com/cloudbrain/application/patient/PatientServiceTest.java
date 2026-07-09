package com.cloudbrain.application.patient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.domain.auth.PatientProfile;
import com.cloudbrain.dto.patient.PatientUpdateRequest;
import com.cloudbrain.repository.IdentityRepository;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.ActorRole;
import com.cloudbrain.security.RolePolicy;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PatientServiceTest {

    private IdentityRepository identityRepository;
    private RolePolicy rolePolicy;
    private PatientService patientService;

    @BeforeEach
    void setUp() {
        identityRepository = org.mockito.Mockito.mock(IdentityRepository.class);
        rolePolicy = org.mockito.Mockito.mock(RolePolicy.class);
        patientService = new PatientService(identityRepository, rolePolicy);
    }

    @Test
    void getCurrentPatientInfoReturnsCurrentPatientProfile() {
        ActorContext actor = patientActor();
        when(rolePolicy.canViewMedicalRecord(actor, 5001L)).thenReturn(true);
        when(identityRepository.findPatientProfile(5001L)).thenReturn(Optional.of(profile()));

        var response = patientService.getCurrentPatientInfo(actor);

        assertThat(response.patientId()).isEqualTo(5001L);
        assertThat(response.realName()).isEqualTo("张三");
        assertThat(response.phone()).isEqualTo("13900000000");
    }

    @Test
    void updateCurrentPatientInfoKeepsFallbackValuesForBlankFields() {
        ActorContext actor = patientActor();
        when(rolePolicy.canViewMedicalRecord(actor, 5001L)).thenReturn(true);
        when(identityRepository.findPatientProfile(5001L)).thenReturn(Optional.of(profile()));
        when(identityRepository.savePatientProfile(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = patientService.updateCurrentPatientInfo(actor,
                new PatientUpdateRequest("  ", "女", null, "13800000000", null, "高血压", "复诊"));

        assertThat(response.realName()).isEqualTo("张三");
        assertThat(response.gender()).isEqualTo("女");
        assertThat(response.age()).isEqualTo(35);
        assertThat(response.phone()).isEqualTo("13800000000");
        ArgumentCaptor<PatientProfile> captor = ArgumentCaptor.forClass(PatientProfile.class);
        verify(identityRepository).savePatientProfile(captor.capture());
        assertThat(captor.getValue().medicalHistory()).isEqualTo("高血压");
    }

    @Test
    void requirePatientPermissionRejectsNonPatientAndMissingProfile() {
        ActorContext doctor = new ActorContext(20L, ActorRole.DOCTOR, null, 2001L, "doctor01", "医生");

        assertThatThrownBy(() -> patientService.getCurrentPatientInfo(doctor))
                .isInstanceOf(ApiException.class)
                .hasMessage("patient permission required");

        ActorContext actor = patientActor();
        when(rolePolicy.canViewMedicalRecord(actor, 5001L)).thenReturn(true);
        when(identityRepository.findPatientProfile(5001L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> patientService.getCurrentPatientInfo(actor))
                .isInstanceOf(ApiException.class)
                .hasMessage("patient profile not found");
    }

    @Test
    void requirePatientPermissionRejectsPolicyDenial() {
        ActorContext actor = patientActor();
        when(rolePolicy.canViewMedicalRecord(actor, 5001L)).thenReturn(false);

        assertThatThrownBy(() -> patientService.getCurrentPatientInfo(actor))
                .isInstanceOf(ApiException.class)
                .hasMessage("patient permission required");
    }

    private ActorContext patientActor() {
        return new ActorContext(10L, ActorRole.PATIENT, 5001L, null, "patient01", "张三");
    }

    private PatientProfile profile() {
        return new PatientProfile(5001L, "patient01", "张三", "男", 35, "13900000000", "110101",
                "无", "备注");
    }
}
