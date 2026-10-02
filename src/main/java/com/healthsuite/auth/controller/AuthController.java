package com.healthsuite.auth.controller;

import com.healthsuite.auth.dto.request.DoctorRegisterRequest;
import com.healthsuite.auth.dto.request.LoginRequest;
import com.healthsuite.auth.dto.request.RefreshTokenRequest;
import com.healthsuite.auth.dto.request.RegisterRequest;
import com.healthsuite.auth.dto.request.SocialAuthRequest;
import com.healthsuite.auth.dto.response.AuthResponse;
import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.auth.service.AuthService;
import com.healthsuite.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, social auth, and token refresh")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new account", description = "Creates a LOCAL auth account. Phone must be a valid Bangladeshi mobile number.")
    @SecurityRequirements   // no auth needed
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully", authService.register(request)));
    }

    @Operation(summary = "Register as a doctor", description = "Creates a user account and submits a doctor profile for admin approval in one step.")
    @SecurityRequirements
    @PostMapping("/register/doctor")
    public ResponseEntity<ApiResponse<AuthResponse>> registerDoctor(@Valid @RequestBody DoctorRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Doctor account created. Pending admin approval.", authService.registerDoctor(request)));
    }

    @Operation(summary = "Login with email and password")
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "Social login / registration",
            description = "Verifies a Google or Facebook token. Enforces unified account rule: same email via a different provider returns 409.")
    @SecurityRequirements
    @PostMapping("/social")
    public ResponseEntity<ApiResponse<AuthResponse>> socialAuth(@Valid @RequestBody SocialAuthRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.socialAuth(request)));
    }

    @Operation(summary = "Rotate refresh token", description = "Invalidates the old refresh token and issues a new access + refresh token pair.")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }
}
