package com.healthsuite.auth.controller;

import com.healthsuite.auth.dto.request.UpdateProfileRequest;
import com.healthsuite.auth.dto.response.UserProfileResponse;
import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.auth.service.UserService;
import com.healthsuite.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "View and update the authenticated user's profile")
public class ProfileController {

    private final UserService userService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                UserProfileResponse.from(userService.findById(principal.getId()))));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        userService.updateProfile(principal.getId(), request.fullName(), request.fcmToken());
        return ResponseEntity.ok(ApiResponse.ok("Profile updated",
                UserProfileResponse.from(userService.findById(principal.getId()))));
    }

    @PostMapping(value = "/profile/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UserProfileResponse>> uploadProfilePhoto(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Profile photo updated",
                UserProfileResponse.from(userService.updateProfilePhoto(principal.getId(), file))));
    }
}
