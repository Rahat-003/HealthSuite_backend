package com.healthsuite.phr.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.phr.dto.request.CreateVisitRequest;
import com.healthsuite.phr.dto.response.MedicalVisitResponse;
import com.healthsuite.phr.service.MedicalVisitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/phr")
@RequiredArgsConstructor
@Tag(name = "Personal Health Records (PHR)", description = "Manage medical visits, diagnoses, documents, and medication schedules")
public class MedicalVisitController {

    private final MedicalVisitService visitService;

    @Operation(summary = "Log a new medical visit")
    @PostMapping("/visits")
    public ResponseEntity<ApiResponse<MedicalVisitResponse>> createVisit(
            @Valid @RequestBody CreateVisitRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Visit created", visitService.createVisit(request, principal.getId())));
    }

    @GetMapping("/visits")
    public ResponseEntity<ApiResponse<PagedResponse<MedicalVisitResponse>>> getMyVisits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(visitService.getMyVisits(principal.getId(),
                PageRequest.of(page, size, Sort.by("visitDate").descending()))));
    }

    @Operation(summary = "Get a specific visit",
            description = "Access granted if requester is the owner, a verified family member, or presents a valid X-Share-Token header.")
    @SecurityRequirements({
            @SecurityRequirement(name = "bearerAuth"),
            @SecurityRequirement(name = "shareToken")
    })
    @GetMapping("/visits/{visitId}")
    public ResponseEntity<ApiResponse<MedicalVisitResponse>> getVisit(
            @PathVariable Long visitId,
            @AuthenticationPrincipal UserPrincipal principal,
            Authentication authentication) {
        Long requesterId = (principal != null) ? principal.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(visitService.getVisit(visitId, requesterId, authentication)));
    }

    @PostMapping(value = "/visits/{visitId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MedicalVisitResponse>> uploadDocument(
            @PathVariable Long visitId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded",
                visitService.attachDocument(visitId, principal.getId(), file)));
    }

    @DeleteMapping("/visits/{visitId}")
    public ResponseEntity<ApiResponse<Void>> deleteVisit(
            @PathVariable Long visitId,
            @AuthenticationPrincipal UserPrincipal principal) {
        visitService.deleteVisit(visitId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Visit deleted"));
    }

    @Operation(summary = "Delete a document from a visit")
    @DeleteMapping("/visits/{visitId}/documents/{documentId}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long visitId,
            @PathVariable Long documentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        visitService.deleteDocument(visitId, documentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Document deleted"));
    }

    // Share-token access path — authentication is ShareTokenAuthentication set by filter
    @GetMapping("/shared/visit/{visitId}")
    public ResponseEntity<ApiResponse<MedicalVisitResponse>> getSharedVisit(
            @PathVariable Long visitId,
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.ok(visitService.getVisit(visitId, null, authentication)));
    }

    // Family proxy — view another user's full history
    @GetMapping("/users/{targetUserId}/visits")
    public ResponseEntity<ApiResponse<PagedResponse<MedicalVisitResponse>>> getFamilyMemberVisits(
            @PathVariable Long targetUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(visitService.getFamilyMemberVisits(
                targetUserId, principal.getId(),
                PageRequest.of(page, size, Sort.by("visitDate").descending()))));
    }
}
