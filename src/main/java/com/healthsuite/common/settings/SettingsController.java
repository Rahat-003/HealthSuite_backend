package com.healthsuite.common.settings;

import com.healthsuite.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Platform Settings", description = "Read platform-wide settings; admin-only to change")
public class SettingsController {

    private final PlatformSettingsService settingsService;

    @GetMapping("/api/settings")
    public ResponseEntity<ApiResponse<PlatformSettingsResponse>> getSettings() {
        return ResponseEntity.ok(ApiResponse.ok(PlatformSettingsResponse.from(settingsService.get())));
    }

    @PutMapping("/api/admin/settings")
    public ResponseEntity<ApiResponse<PlatformSettingsResponse>> updateSettings(
            @Valid @RequestBody UpdatePlatformSettingsRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Settings updated",
            PlatformSettingsResponse.from(settingsService.update(request))));
    }
}
