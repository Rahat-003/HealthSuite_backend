package com.healthsuite.marketplace.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.marketplace.dto.request.CreateConsultationRequest;
import com.healthsuite.marketplace.dto.request.DoctorRegistrationRequest;
import com.healthsuite.marketplace.dto.response.ConsultationMediaResponse;
import com.healthsuite.marketplace.dto.response.ConsultationRequestResponse;
import com.healthsuite.marketplace.dto.response.DoctorDocumentResponse;
import com.healthsuite.marketplace.dto.response.DoctorProfileResponse;
import com.healthsuite.marketplace.dto.response.SpecialtyResponse;
import com.healthsuite.marketplace.entity.ConsultationMedia;
import com.healthsuite.marketplace.enums.ConsultationMediaType;
import com.healthsuite.marketplace.enums.DoctorDocumentType;
import com.healthsuite.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
@RequiredArgsConstructor
@Tag(name = "Marketplace", description = "Doctor marketplace: browsing, registration, consultations")
public class MarketplaceController {

    private final MarketplaceService marketplaceService;

    // ── Specialty catalog (public) ────────────────────────────────────────

    @GetMapping("/specialties")
    public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> listSpecialties() {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.listSpecialties()));
    }

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

    @PutMapping("/doctors/me/availability")
    public ResponseEntity<ApiResponse<DoctorProfileResponse>> setAvailability(
            @RequestParam("available") boolean available,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
            available ? "You are now accepting new cases." : "You are now unavailable.",
            marketplaceService.setAvailability(principal.getId(), available)));
    }

    @PostMapping(value = "/doctors/me/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DoctorDocumentResponse>> uploadDoctorDocument(
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") DoctorDocumentType type,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.ok("Document uploaded.",
                marketplaceService.uploadDoctorDocument(principal.getId(), file, type)));
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

    @PutMapping("/consultations/{id}/cancel")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> cancelConsultation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Request cancelled and refunded",
            marketplaceService.cancelConsultation(id, principal.getId())));
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

    @GetMapping("/consultations/{id}/media/{mediaId}/download")
    public ResponseEntity<Resource> downloadMedia(
            @PathVariable Long id,
            @PathVariable Long mediaId,
            @AuthenticationPrincipal UserPrincipal principal) throws IOException {
        ConsultationMedia media = marketplaceService.getMediaForDownload(id, mediaId, principal.getId());

        Path path = Paths.get(media.getFilePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Media file missing on disk", mediaId);
        }
        String contentType = Files.probeContentType(path);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + media.getFileName() + "\"")
            .body(new FileSystemResource(path));
    }
}
