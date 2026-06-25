package com.cloudbrain.application.auth;

import com.cloudbrain.common.exception.ApiException;
import com.cloudbrain.domain.auth.AccountProfile;
import com.cloudbrain.dto.auth.LoginRequest;
import com.cloudbrain.dto.auth.LoginResponse;
import com.cloudbrain.dto.auth.LogoutRequest;
import com.cloudbrain.dto.auth.RefreshRequest;
import com.cloudbrain.dto.auth.RegisterRequest;
import com.cloudbrain.repository.IdentityRepository;
import com.cloudbrain.repository.AuditLogJpaRepository;
import com.cloudbrain.repository.SessionTokenJpaRepository;
import com.cloudbrain.entity.auth.AuditLogEntity;
import com.cloudbrain.entity.auth.SessionTokenEntity;
import com.cloudbrain.security.ActorContext;
import com.cloudbrain.security.ActorRole;
import com.cloudbrain.security.JwtTokenUtil;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String ACTIVE = "ACTIVE";
    private static final String REVOKED = "REVOKED";

    private final IdentityRepository identityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final SessionTokenJpaRepository sessionTokenRepository;
    private final AuditLogJpaRepository auditLogRepository;

    public AuthService(IdentityRepository identityRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenUtil jwtTokenUtil,
                       SessionTokenJpaRepository sessionTokenRepository,
                       AuditLogJpaRepository auditLogRepository) {
        this.identityRepository = identityRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenUtil = jwtTokenUtil;
        this.sessionTokenRepository = sessionTokenRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void patientRegister(RegisterRequest request) {
        if (request.age() != null && request.age() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST.value(), "age must be positive");
        }
        identityRepository.registerPatient(request, passwordEncoder.encode(request.password()));
    }

    @Transactional
    public LoginResponse patientLogin(LoginRequest request) {
        return login(request, ActorRole.PATIENT);
    }

    @Transactional
    public LoginResponse doctorLogin(LoginRequest request) {
        return login(request, ActorRole.DOCTOR);
    }

    @Transactional
    public LoginResponse adminLogin(LoginRequest request) {
        return login(request, ActorRole.ADMIN);
    }

    @Transactional
    public LoginResponse refresh(RefreshRequest request) {
        SessionTokenEntity session = sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken(request.refreshToken()), ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "login expired please re-login"));
        if (session.getRefreshExpiresAt() == null || session.getRefreshExpiresAt().isBefore(Instant.now())) {
            revokeSession(session, "refresh token expired");
            recordAudit(session, "REFRESH", false, "refresh token expired");
            throw new ApiException(HttpStatus.UNAUTHORIZED.value(), "login expired please re-login");
        }
        AccountProfile account = identityRepository.findByUserId(session.getUserId())
                .orElseThrow(() -> {
                    recordAudit(session, "REFRESH", false, "account not found");
                    return new ApiException(HttpStatus.UNAUTHORIZED.value(), "login expired please re-login");
                });
        String newRefreshToken = UUID.randomUUID().toString();
        return issueSession(account, session, newRefreshToken, "refresh");
    }

    @Transactional
    public void logout(LogoutRequest request) {
        SessionTokenEntity session = sessionTokenRepository.findByRefreshHashAndStatus(jwtTokenUtil.hashToken(request.refreshToken()), ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "login expired please re-login"));
        revokeSession(session, firstNonBlank(request.reason(), "logout"));
        recordAudit(session, "LOGOUT", true, firstNonBlank(request.reason(), "logout"));
    }

    private LoginResponse login(LoginRequest request, ActorRole expectedRole) {
        AccountProfile account = identityRepository.findByUsername(request.username())
                .filter(profile -> profile.role() == expectedRole)
                .filter(profile -> passwordEncoder.matches(request.password(), profile.passwordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED.value(), "invalid username or password"));

        return issueSession(account, null, UUID.randomUUID().toString(), "login");
    }

    private LoginResponse issueSession(AccountProfile account, SessionTokenEntity existingSession, String refreshToken, String action) {
        ActorContext actorContext = account.toActorContext();
        String tokenId = existingSession == null ? UUID.randomUUID().toString() : existingSession.getTokenId();
        String token = jwtTokenUtil.generateToken(actorContext, tokenId);
        Instant tokenExpiresAt = Instant.now().plus(jwtTokenUtil.getTokenTtl());
        Instant refreshExpiresAt = Instant.now().plus(jwtTokenUtil.getRefreshTokenTtl());

        SessionTokenEntity session = existingSession == null ? new SessionTokenEntity() : existingSession;
        session.setTokenId(tokenId);
        session.setUserId(account.userId());
        session.setRole(account.role().name());
        session.setPatientId(account.patientId());
        session.setDoctorId(account.doctorId());
        session.setUsername(account.username());
        session.setDisplayName(account.displayName());
        session.setTokenHash(jwtTokenUtil.hashToken(token));
        session.setRefreshHash(jwtTokenUtil.hashToken(refreshToken));
        session.setExpiresAt(tokenExpiresAt);
        session.setRefreshExpiresAt(refreshExpiresAt);
        session.setStatus(ACTIVE);
        session.setRevokedAt(null);
        session.setLogoutReason(null);
        sessionTokenRepository.save(session);

        LoginResponse response = new LoginResponse(
                token,
                refreshToken,
                "Bearer",
                account.userId(),
                account.role().name().toLowerCase(Locale.ROOT),
                account.patientId(),
                account.doctorId(),
                account.username(),
                account.displayName(),
                tokenExpiresAt.toEpochMilli()
        );
        recordAudit(
                response.userId(),
                response.role(),
                action.toUpperCase(Locale.ROOT),
                true,
                account.username(),
                "AUTH_SESSION",
                account.userId(),
                tokenId
        );
        return response;
    }

    private void revokeSession(SessionTokenEntity session, String reason) {
        session.setStatus(REVOKED);
        session.setRevokedAt(Instant.now());
        session.setLogoutReason(reason);
        sessionTokenRepository.save(session);
    }

    private void recordAudit(Long actorId, String actorRole, String action, boolean success, String message) {
        recordAudit(actorId, actorRole, action, success, message, null, null, null);
    }

    private void recordAudit(SessionTokenEntity session, String action, boolean success, String message) {
        recordAudit(session.getUserId(), session.getRole(), action, success, message, "AUTH_SESSION", session.getUserId(), session.getTokenId());
    }

    private void recordAudit(Long actorId,
                             String actorRole,
                             String action,
                             boolean success,
                             String message,
                             String resourceType,
                             Long resourceId,
                             String traceId) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.setActorId(actorId);
        auditLog.setActorRole(actorRole);
        auditLog.setAction(action);
        auditLog.setResourceType(resourceType);
        auditLog.setResourceId(resourceId);
        auditLog.setTraceId(traceId);
        auditLog.setSuccess(success);
        auditLog.setMessage(message);
        auditLog.setOccurredAt(Instant.now());
        auditLogRepository.save(auditLog);
    }

    private String firstNonBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
