package com.cloudbrain.dto.auth;

public record LoginResponse(
        String token,
        String refreshToken,
        String tokenType,
        Long userId,
        String role,
        Long patientId,
        Long doctorId,
        String username,
        String displayName,
        long expiresAt
) {
}
