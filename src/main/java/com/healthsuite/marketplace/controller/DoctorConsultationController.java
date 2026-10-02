package com.healthsuite.marketplace.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.marketplace.dto.response.ConsultationRequestResponse;
import com.healthsuite.marketplace.service.DoctorPatientsService;
import com.healthsuite.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor/consultations")
@RequiredArgsConstructor
@Tag(name = "Doctor — Consultations", description = "Doctor-side triage queue: first response claims the case")
public class DoctorConsultationController {

    private final MarketplaceService marketplaceService;
    private final DoctorPatientsService doctorPatientsService;

    @GetMapping("/patients")
    public ResponseEntity<ApiResponse<List<DoctorPatientsService.PatientSummary>>> getMyPatients(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(doctorPatientsService.getMyPatients(principal.getId())));
    }

    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<List<ConsultationRequestResponse>>> getQueue(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.getDoctorQueue(principal.getId())));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ConsultationRequestResponse>>> getActiveCases(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.getDoctorActiveCases(principal.getId())));
    }

    @PutMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> acceptCase(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Case accepted — it's yours.",
            marketplaceService.acceptCase(id, principal.getId())));
    }

    @PutMapping("/{id}/pass")
    public ResponseEntity<ApiResponse<Void>> passCase(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        marketplaceService.passCase(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Case passed.", null));
    }
}
