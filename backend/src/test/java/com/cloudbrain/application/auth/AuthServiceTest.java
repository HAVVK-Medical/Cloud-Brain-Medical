package com.cloudbrain.application.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.domain.auth.AccountProfile;
import com.cloudbrain.dto.auth.LoginRequest;
import com.cloudbrain.dto.auth.LogoutRequest;
import com.cloudbrain.dto.auth.RefreshRequest;
import com.cloudbrain.dto.auth.RegisterRequest;
import com.cloudbrain.entity.auth.AuditLogEntity;
import com.cloudbrain.entity.auth.SessionTokenEntity;
import com.cloudbrain.repository.AuditLogJpaRepository;
import com.cloudbrain.repository.IdentityRepository;
import com.cloudbrain.repository.SessionTokenJpaRepository;
import com.cloudbrain.security.ActorRole;
import com.cloudbrain.security.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private IdentityRepository identityRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenUtil jwtTokenUtil;
    private SessionTokenJpaRepository sessionTokenRepository;
    private AuditLogJpaRepository auditLogRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        identityRepository = org.mockito.Mockito.mock(IdentityRepository.class);
        passwordEncoder = org.mockito.Mockito.mock(PasswordEncoder.class);
        jwtTokenUtil = new JwtTokenUtil(new ObjectMapper(), "unit-test-secret-that-is-long-enough", 60, 120);
        sessionTokenRepository = org.mockito.Mockito.mock(SessionTokenJpaRepository.class);
        auditLogRepository = org.mockito.Mockito.mock(AuditLogJpaRepository.class);
        authService = new AuthService(identityRepository, passwordEncoder, jwtTokenUtil, sessionTokenRepository, auditLogRepository);
    }

    @Test
    void patientRegisterRejectsNegativeAgeAndDoesNotPersist() {
        RegisterRequest request = new RegisterRequest("alice", "pw", "Alice", "13900000000", "F", -1);

        assertThatThrownBy(() -> authService.patientRegister(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("age must be positive");
    }

    @Test
    void patientRegisterEncodesPasswordAndDelegatesToIdentityRepository() {
        RegisterRequest request = new RegisterRequest("alice", "pw", "Alice", "13900000000", "F", 20);
        when(passwordEncoder.encode("pw")).thenReturn("{bcrypt}encoded");

        authService.patientRegister(request);

        verify(identityRepository).registerPatient(request, "{bcrypt}encoded");
    }

    @Test
    void patientLoginIssuesSessionAndAuditLogForMatchingRole() {
        AccountProfile account = account(10L, ActorRole.PATIENT, 5001L, null);
        when(identityRepository.findByUsername("patient01")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("patient123", "{noop}patient123")).thenReturn(true);

        var response = authService.patientLogin(new LoginRequest("patient01", "patient123"));

        assertThat(response.token()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.role()).isEqualTo("patient");
        assertThat(response.patientId()).isEqualTo(5001L);
        ArgumentCaptor<SessionTokenEntity> sessionCaptor = ArgumentCaptor.forClass(SessionTokenEntity.class);
        verify(sessionTokenRepository).save(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(sessionCaptor.getValue().getRefreshHash()).isEqualTo(jwtTokenUtil.hashToken(response.refreshToken()));
        verify(auditLogRepository).save(any(AuditLogEntity.class));
    }

    @Test
    void doctorAndAdminLoginIssueRoleSpecificSessions() {
        AccountProfile doctor = account(20L, ActorRole.DOCTOR, null, 2001L);
        AccountProfile admin = account(30L, ActorRole.ADMIN, null, null);
        when(identityRepository.findByUsername("doctor01")).thenReturn(Optional.of(doctor));
        when(identityRepository.findByUsername("admin01")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("doctor123", "{noop}doctor123")).thenReturn(true);
        when(passwordEncoder.matches("admin123", "{noop}admin123")).thenReturn(true);

        var doctorLogin = authService.doctorLogin(new LoginRequest("doctor01", "doctor123"));
        var adminLogin = authService.adminLogin(new LoginRequest("admin01", "admin123"));

        assertThat(doctorLogin.role()).isEqualTo("doctor");
        assertThat(doctorLogin.doctorId()).isEqualTo(2001L);
        assertThat(adminLogin.role()).isEqualTo("admin");
        assertThat(adminLogin.patientId()).isNull();
        assertThat(adminLogin.doctorId()).isNull();
    }

    @Test
    void loginRejectsWrongRoleOrPassword() {
        when(identityRepository.findByUsername("doctor01"))
                .thenReturn(Optional.of(account(20L, ActorRole.DOCTOR, null, 2001L)));
        when(passwordEncoder.matches(eq("bad"), any())).thenReturn(false);

        assertThatThrownBy(() -> authService.patientLogin(new LoginRequest("doctor01", "doctor123")))
                .isInstanceOf(ApiException.class)
                .hasMessage("invalid username or password");
        assertThatThrownBy(() -> authService.doctorLogin(new LoginRequest("doctor01", "bad")))
                .isInstanceOf(ApiException.class)
                .hasMessage("invalid username or password");
    }

    @Test
    void refreshRotatesRefreshTokenForActiveSession() {
        SessionTokenEntity session = activeSession();
        when(sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken("refresh-old"), "ACTIVE"))
                .thenReturn(Optional.of(session));
        when(identityRepository.findByUserId(10L)).thenReturn(Optional.of(account(10L, ActorRole.PATIENT, 5001L, null)));

        var response = authService.refresh(new RefreshRequest("refresh-old"));

        assertThat(response.refreshToken()).isNotEqualTo("refresh-old");
        assertThat(response.role()).isEqualTo("patient");
        assertThat(session.getStatus()).isEqualTo("ACTIVE");
        assertThat(session.getRefreshHash()).isEqualTo(jwtTokenUtil.hashToken(response.refreshToken()));
    }

    @Test
    void refreshRevokesExpiredRefreshToken() {
        SessionTokenEntity session = activeSession();
        session.setRefreshExpiresAt(Instant.now().minusSeconds(1));
        when(sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken("expired"), "ACTIVE"))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("expired")))
                .isInstanceOf(ApiException.class)
                .hasMessage("login expired please re-login");
        assertThat(session.getStatus()).isEqualTo("REVOKED");
        assertThat(session.getLogoutReason()).isEqualTo("refresh token expired");
    }

    @Test
    void refreshRevokesSessionWhenRefreshExpiryIsMissing() {
        SessionTokenEntity session = activeSession();
        session.setRefreshExpiresAt(null);
        when(sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken("missing-expiry"), "ACTIVE"))
                .thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("missing-expiry")))
                .isInstanceOf(ApiException.class)
                .hasMessage("login expired please re-login");

        assertThat(session.getStatus()).isEqualTo("REVOKED");
        assertThat(session.getLogoutReason()).isEqualTo("refresh token expired");
    }

    @Test
    void refreshAndLogoutRejectUnknownRefreshToken() {
        when(sessionTokenRepository.findByRefreshHashAndStatus(any(), eq("ACTIVE"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("unknown")))
                .isInstanceOf(ApiException.class)
                .hasMessage("login expired please re-login");
        assertThatThrownBy(() -> authService.logout(new LogoutRequest("unknown", null)))
                .isInstanceOf(ApiException.class)
                .hasMessage("login expired please re-login");
    }

    @Test
    void refreshAuditsAndRejectsWhenAccountNoLongerExists() {
        SessionTokenEntity session = activeSession();
        when(sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken("refresh-old"), "ACTIVE"))
                .thenReturn(Optional.of(session));
        when(identityRepository.findByUserId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh(new RefreshRequest("refresh-old")))
                .isInstanceOf(ApiException.class)
                .hasMessage("login expired please re-login");

        ArgumentCaptor<AuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("REFRESH");
        assertThat(auditCaptor.getValue().getSuccess()).isFalse();
        assertThat(auditCaptor.getValue().getMessage()).isEqualTo("account not found");
    }

    @Test
    void logoutRevokesActiveSessionWithReason() {
        SessionTokenEntity session = activeSession();
        when(sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken("refresh-old"), "ACTIVE"))
                .thenReturn(Optional.of(session));

        authService.logout(new LogoutRequest("refresh-old", "manual logout"));

        assertThat(session.getStatus()).isEqualTo("REVOKED");
        assertThat(session.getLogoutReason()).isEqualTo("manual logout");
        verify(sessionTokenRepository).save(session);
    }

    @Test
    void logoutUsesDefaultReasonWhenReasonIsBlank() {
        SessionTokenEntity session = activeSession();
        when(sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken("refresh-old"), "ACTIVE"))
                .thenReturn(Optional.of(session));

        authService.logout(new LogoutRequest("refresh-old", " "));

        assertThat(session.getStatus()).isEqualTo("REVOKED");
        assertThat(session.getLogoutReason()).isEqualTo("logout");
    }

    @Test
    void logoutUsesDefaultReasonWhenReasonIsNull() {
        SessionTokenEntity session = activeSession();
        when(sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken("refresh-old"), "ACTIVE"))
                .thenReturn(Optional.of(session));

        authService.logout(new LogoutRequest("refresh-old", null));

        assertThat(session.getStatus()).isEqualTo("REVOKED");
        assertThat(session.getLogoutReason()).isEqualTo("logout");
    }

    @Test
    void recordAuditConvenienceOverloadPersistsBasicAuditLog() throws Exception {
        Method method = AuthService.class.getDeclaredMethod(
                "recordAudit",
                Long.class,
                String.class,
                String.class,
                boolean.class,
                String.class
        );
        method.setAccessible(true);

        method.invoke(authService, 99L, "ADMIN", "MANUAL", true, "ok");

        ArgumentCaptor<AuditLogEntity> auditCaptor = ArgumentCaptor.forClass(AuditLogEntity.class);
        verify(auditLogRepository).save(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getActorId()).isEqualTo(99L);
        assertThat(auditCaptor.getValue().getActorRole()).isEqualTo("ADMIN");
        assertThat(auditCaptor.getValue().getAction()).isEqualTo("MANUAL");
        assertThat(auditCaptor.getValue().getMessage()).isEqualTo("ok");
    }

    private AccountProfile account(Long userId, ActorRole role, Long patientId, Long doctorId) {
        return new AccountProfile(userId, role, role.name().toLowerCase() + "01", "{noop}" + role.name().toLowerCase() + "123",
                patientId, doctorId, role.name() + " 用户", "13900000000");
    }

    private SessionTokenEntity activeSession() {
        SessionTokenEntity session = new SessionTokenEntity();
        session.setTokenId("token-id");
        session.setUserId(10L);
        session.setRole("PATIENT");
        session.setPatientId(5001L);
        session.setUsername("patient01");
        session.setDisplayName("患者一");
        session.setTokenHash("old-token-hash");
        session.setRefreshHash(jwtTokenUtil.hashToken("refresh-old"));
        session.setExpiresAt(Instant.now().plusSeconds(3600));
        session.setRefreshExpiresAt(Instant.now().plusSeconds(7200));
        session.setStatus("ACTIVE");
        return session;
    }
}
