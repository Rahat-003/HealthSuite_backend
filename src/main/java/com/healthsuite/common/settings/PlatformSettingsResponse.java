package com.healthsuite.common.settings;

public record PlatformSettingsResponse(
    int maxDoctorsPerConsultation,
    int consultationCancelWaitHours
) {
    public static PlatformSettingsResponse from(PlatformSettings s) {
        return new PlatformSettingsResponse(s.getMaxDoctorsPerConsultation(), s.getConsultationCancelWaitHours());
    }
}
