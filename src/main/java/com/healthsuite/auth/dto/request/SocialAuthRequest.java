package com.healthsuite.auth.dto.request;

import com.healthsuite.auth.enums.AuthProvider;
import com.healthsuite.common.validation.BangladeshPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SocialAuthRequest(
        @NotNull AuthProvider provider,
        @NotBlank String idToken,
        // Phone is mandatory on first social registration since BD phone is required for all users
        @NotBlank @BangladeshPhone String phoneNumber,
        @NotBlank String fullName
) {}
