package com.cloudbrain.controller;

import com.cloudbrain.application.auth.AuthService;
import com.cloudbrain.common.Result;
import com.cloudbrain.dto.auth.LoginRequest;
import com.cloudbrain.dto.auth.LoginResponse;
import com.cloudbrain.dto.auth.LogoutRequest;
import com.cloudbrain.dto.auth.RefreshRequest;
import com.cloudbrain.dto.auth.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/auth", "/api"})
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/patient/register")
    public Result<Void> patientRegister(@Valid @RequestBody RegisterRequest request) {
        authService.patientRegister(request);
        return Result.success("registered", null);
    }

    @PostMapping("/patient/login")
    public Result<LoginResponse> patientLogin(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.patientLogin(request));
    }

    @PostMapping("/doctor/login")
    public Result<LoginResponse> doctorLogin(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.doctorLogin(request));
    }

    @PostMapping("/admin/login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.adminLogin(request));
    }

    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return Result.success(authService.refresh(request));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
        return Result.success("logged out", null);
    }
}
