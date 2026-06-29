package com.healthsuite.family.dto.request;

import com.healthsuite.common.validation.BangladeshPhone;
import jakarta.validation.constraints.NotBlank;

public record AddFamilyMemberRequest(
        @NotBlank @BangladeshPhone String phoneNumber
) {}
