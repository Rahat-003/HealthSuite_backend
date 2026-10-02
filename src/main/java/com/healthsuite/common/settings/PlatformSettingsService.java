package com.healthsuite.common.settings;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PlatformSettingsService {

    private static final long SINGLETON_ID = 1L;

    private final PlatformSettingsRepository repository;

    /** Always returns the singleton row, seeding it if the migration insert was somehow bypassed. */
    @Transactional
    public PlatformSettings get() {
        return repository.findById(SINGLETON_ID)
            .orElseGet(() -> repository.save(PlatformSettings.builder().id(SINGLETON_ID).build()));
    }

    @Transactional
    public PlatformSettings update(UpdatePlatformSettingsRequest req) {
        PlatformSettings settings = get();
        settings.setMaxDoctorsPerConsultation(req.maxDoctorsPerConsultation());
        settings.setConsultationCancelWaitHours(req.consultationCancelWaitHours());
        return repository.save(settings);
    }
}
