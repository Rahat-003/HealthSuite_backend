package com.healthsuite.family.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

/** scope NONE removes the grant entirely. visitIds only apply to SELECTED. */
public record UpdateRecordShareRequest(
        @NotBlank @Pattern(regexp = "ALL|SELECTED|NONE") String scope,
        List<Long> visitIds
) {}
