package com.healthsuite.marketplace.controller;

import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.marketplace.dto.request.AdminDecisionRequest;
import com.healthsuite.marketplace.dto.response.DoctorProfileResponse;
import com.healthsuite.marketplace.enums.DoctorStatus;
import com.healthsuite.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/marketplace")
@RequiredArgsConstructor
@Tag(name = "Admin — Marketplace", description = "Admin operations for doctor approval and marketplace management")
public class AdminMarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<PagedResponse<DoctorProfileResponse>>> listDoctors(
            @RequestParam(defaultValue = "PENDING") DoctorStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
            marketplaceService.adminListDoctors(status, PageRequest.of(page, size))
        ));
    }

    @PutMapping("/doctors/{id}/approve")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> approveDoctor(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Doctor approved", marketplaceService.approveDoctor(id)));
    }

    @PutMapping("/doctors/{id}/reject")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> rejectDoctor(
            @PathVariable Long id,
            @RequestBody(required = false) AdminDecisionRequest body) {
        String reason = body != null ? body.reason() : null;
        return ResponseEntity.ok(ApiResponse.ok("Doctor rejected", marketplaceService.rejectDoctor(id, reason)));
    }
}
