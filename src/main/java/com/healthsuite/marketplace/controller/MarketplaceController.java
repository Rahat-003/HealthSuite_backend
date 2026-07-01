package com.healthsuite.marketplace.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.marketplace.dto.request.CreateConsultationRequest;
import com.healthsuite.marketplace.dto.request.DoctorRegistrationRequest;
import com.healthsuite.marketplace.dto.response.ConsultationMediaResponse;
import com.healthsuite.marketplace.dto.response.ConsultationRequestResponse;
import com.healthsuite.marketplace.dto.response.DoctorProfileResponse;
import com.healthsuite.marketplace.enums.ConsultationMediaType;
import com.healthsuite.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Doctor marketplace: browsing, registration, consultations")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    // ── Doctor listing (public) ───────────────────────────────────────────

    @GetMapping("/doctors")
    public ResponseEntity<ApiResponse<PagedResponse<DoctorProfileResponse>>> listDoctors(
            @RequestParam(required = false) String specialty,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return ResponseEntity.ok(ApiResponse.ok(
            marketplaceService.listDoctors(specialty, search, PageRequest.of(page, size))
        ));
    }

    @GetMapping("/doctors/{id}")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> getDoctorById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.getDoctorById(id)));
    }

    // ── Doctor self-registration ──────────────────────────────────────────

    @PostMapping("/doctors/register")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> registerAsDoctor(
            @Valid @RequestBody DoctorRegistrationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Application submitted. Pending admin approval.",
                marketplaceService.registerDoctor(request, principal.getId())));
    }

    @GetMapping("/doctors/me")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.getMyDoctorProfile(principal.getId())));
    }

    // ── Consultation requests ─────────────────────────────────────────────

    @PostMapping("/consultations")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> createConsultation(
            @Valid @RequestBody CreateConsultationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Consultation request sent.",
                marketplaceService.createConsultation(request, principal.getId())));
    }

    @GetMapping("/consultations/mine")
    public ResponseEntity<ApiResponse<List<ConsultationRequestResponse>>> getMyConsultations(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.getMyConsultations(principal.getId())));
    }

    @GetMapping("/consultations/{id}")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> getConsultation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.getConsultation(id, principal.getId())));
    }

    @PostMapping(value = "/consultations/{id}/media", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ConsultationMediaResponse>> uploadMedia(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") ConsultationMediaType type,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Media uploaded successfully.",
                marketplaceService.uploadMedia(id, principal.getId(), file, type)));
    }
}
