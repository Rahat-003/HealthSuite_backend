package com.healthsuite.common.settings;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdatePlatformSettingsRequest(
    @NotNull @Min(1) @Max(10) Integer maxDoctorsPerConsultation,
    @NotNull @Min(0) @Max(168) Integer consultationCancelWaitHours
) {}
