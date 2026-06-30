package com.cloudbrain.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DefaultRolePolicyTest {

    private final DefaultRolePolicy rolePolicy = new DefaultRolePolicy();

    @Test
    void patientCanOnlyViewOwnMedicalRecord() {
        ActorContext patient = actor(10L, ActorRole.PATIENT, 101L, null);

        assertThat(rolePolicy.canViewMedicalRecord(patient, 101L)).isTrue();
        assertThat(rolePolicy.canViewMedicalRecord(patient, 102L)).isFalse();
        assertThat(rolePolicy.canViewMedicalRecord(null, 101L)).isFalse();
        assertThat(rolePolicy.canViewMedicalRecord(patient, null)).isFalse();
    }

    @Test
    void adminCanViewMedicalRecordsAndDoctorDashboards() {
        ActorContext admin = actor(1L, ActorRole.ADMIN, null, null);

        assertThat(rolePolicy.canViewMedicalRecord(admin, 101L)).isTrue();
        assertThat(rolePolicy.canViewDashboardAsDoctor(admin, 202L)).isTrue();
        assertThat(rolePolicy.canViewDashboardAsAdmin(admin)).isTrue();
    }

    @Test
    void doctorWritePermissionsRequireRegistrationId() {
        ActorContext doctor = actor(20L, ActorRole.DOCTOR, null, 202L);
        ActorContext patient = actor(10L, ActorRole.PATIENT, 101L, null);

        assertThat(rolePolicy.canSaveMedicalRecord(doctor, 3001L)).isTrue();
        assertThat(rolePolicy.canSubmitPrescription(doctor, 3001L)).isTrue();
        assertThat(rolePolicy.canSaveMedicalRecord(doctor, null)).isFalse();
        assertThat(rolePolicy.canSubmitPrescription(patient, 3001L)).isFalse();
    }

    @Test
    void doctorDashboardAndNotificationAccessAreScopedToActorIdentity() {
        ActorContext doctor = actor(20L, ActorRole.DOCTOR, null, 202L);
        ActorContext patient = actor(10L, ActorRole.PATIENT, 101L, null);
        ActorContext admin = actor(1L, ActorRole.ADMIN, null, null);

        assertThat(rolePolicy.canViewDashboardAsDoctor(doctor, 202L)).isTrue();
        assertThat(rolePolicy.canViewDashboardAsDoctor(doctor, 203L)).isFalse();
        assertThat(rolePolicy.canViewNotification(doctor, 202L)).isTrue();
        assertThat(rolePolicy.canViewNotification(patient, 101L)).isTrue();
        assertThat(rolePolicy.canViewNotification(patient, 999L)).isFalse();
        assertThat(rolePolicy.canViewNotification(admin, 999L)).isTrue();
        assertThat(rolePolicy.canViewNotification(null, 999L)).isFalse();
        assertThat(rolePolicy.canViewNotification(doctor, null)).isFalse();
    }

    private ActorContext actor(Long userId, ActorRole role, Long patientId, Long doctorId) {
        return new ActorContext(userId, role, patientId, doctorId, "user" + userId, "用户" + userId);
    }
}
