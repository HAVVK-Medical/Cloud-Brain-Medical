package com.cloudbrain.security;

import com.cloudbrain.common.exception.ApiException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper;
    private final String secret;
    private final Duration tokenTtl;
    private final Duration refreshTokenTtl;

    public JwtTokenUtil(ObjectMapper objectMapper,
                        @Value("${app.security.jwt-secret:cloud-brain-medical-dev-secret-change-me}") String secret,
                        @Value("${app.security.jwt-ttl-minutes:720}") long ttlMinutes,
                        @Value("${app.security.refresh-ttl-minutes:10080}") long refreshTtlMinutes) {
        this.objectMapper = objectMapper;
        this.secret = secret;
        this.tokenTtl = Duration.ofMinutes(ttlMinutes);
        this.refreshTokenTtl = Duration.ofMinutes(refreshTtlMinutes);
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public String generateToken(ActorContext actorContext) {
        return generateToken(actorContext, null);
    }

    public String generateToken(ActorContext actorContext, String tokenId) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTtl);
        String resolvedTokenId = tokenId == null || tokenId.isBlank() ? java.util.UUID.randomUUID().toString() : tokenId;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("jti", resolvedTokenId);
        payload.put("sub", actorContext.username());
        payload.put("uid", actorContext.userId());
        payload.put("role", actorContext.role().name());
        payload.put("patientId", actorContext.patientId());
        payload.put("doctorId", actorContext.doctorId());
        payload.put("displayName", actorContext.displayName());
        payload.put("iat", issuedAt.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String header = encodeJson(headerMap());
        String body = encodeJson(payload);
        String signature = sign(header, body);
        return header + "." + body + "." + signature;
    }

    public boolean validateToken(String token) {
        return parseClaims(token).isPresent();
    }

    public Long getUserIdFromToken(String token) {
        return parseClaims(token)
                .map(claims -> toLong(claims.get("uid")))
                .orElseThrow(() -> new ApiException(401, "invalid token"));
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token)
                .map(claims -> String.valueOf(claims.get("role")).toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ApiException(401, "invalid token"));
    }

    public String getTokenIdFromToken(String token) {
        return parseClaims(token)
                .map(claims -> String.valueOf(claims.get("jti")))
                .orElseThrow(() -> new ApiException(401, "invalid token"));
    }

    public Optional<ActorContext> parseActorContext(String token) {
        return parseClaims(token).map(claims -> new ActorContext(
                toLong(claims.get("uid")),
                ActorRole.valueOf(String.valueOf(claims.get("role")).toUpperCase(Locale.ROOT)),
                toNullableLong(claims.get("patientId")),
                toNullableLong(claims.get("doctorId")),
                String.valueOf(claims.get("sub")),
                toNullableString(claims.get("displayName"))
        ));
    }

    private Optional<Map<String, Object>> parseClaims(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3) {
                return Optional.empty();
            }
            if (!MessageDigest.isEqual(signatureBytes(parts[0], parts[1]), BASE64_URL_DECODER.decode(parts[2]))) {
                return Optional.empty();
            }

            Map<String, Object> claims = objectMapper.readValue(decode(parts[1]), MAP_TYPE);
            long expiresAt = toLong(claims.get("exp"));
            if (expiresAt <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    public String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : encoded) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Map<String, Object> headerMap() {
        Map<String, Object> header = new LinkedHashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");
        return header;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to encode token payload", exception);
        }
    }

    private String decode(String base64UrlValue) {
        return new String(BASE64_URL_DECODER.decode(base64UrlValue), StandardCharsets.UTF_8);
    }

    private byte[] signatureBytes(String header, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal((header + "." + payload).getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to sign token", exception);
        }
    }

    private String sign(String header, String payload) {
        return BASE64_URL_ENCODER.encodeToString(signatureBytes(header, payload));
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private Long toNullableLong(Object value) {
        if (value == null || "null".equalsIgnoreCase(String.valueOf(value))) {
            return null;
        }
        return toLong(value);
    }

    private String toNullableString(Object value) {
        if (value == null || "null".equalsIgnoreCase(String.valueOf(value))) {
            return null;
        }
        return String.valueOf(value);
    }
}
