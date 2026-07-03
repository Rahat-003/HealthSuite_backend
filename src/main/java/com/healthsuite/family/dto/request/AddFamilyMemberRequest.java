package com.healthsuite.family.dto.request;

import com.healthsuite.common.validation.BangladeshPhone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddFamilyMemberRequest(
        @NotBlank @BangladeshPhone String phoneNumber,
        @Pattern(regexp = "MOTHER|FATHER|SPOUSE|SON|DAUGHTER|BROTHER|SISTER|GRANDPARENT|GRANDCHILD|RELATIVE|OTHER")
        String relationship
) {}
