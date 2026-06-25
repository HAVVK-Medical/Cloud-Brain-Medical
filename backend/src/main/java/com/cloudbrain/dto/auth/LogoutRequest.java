package com.cloudbrain.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LogoutRequest(
        @NotBlank String refreshToken,
        @Size(max = 255) String reason
) {
}
