package com.healthsuite.auth.controller;

import com.healthsuite.auth.dto.response.UserProfileResponse;
import com.healthsuite.auth.service.UserService;
import com.healthsuite.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin — Users", description = "Admin user management: list, enable, disable accounts")
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> listUsers() {
        return ResponseEntity.ok(ApiResponse.ok(
            userService.listUsers().stream().map(UserProfileResponse::from).toList()));
    }

    @PutMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<UserProfileResponse>> enableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User enabled",
            UserProfileResponse.from(userService.setUserEnabled(id, true))));
    }

    @PutMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<UserProfileResponse>> disableUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("User disabled",
            UserProfileResponse.from(userService.setUserEnabled(id, false))));
    }
}
