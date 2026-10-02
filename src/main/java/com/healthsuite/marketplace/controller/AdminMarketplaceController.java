package com.healthsuite.marketplace.controller;

import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.marketplace.dto.request.AdminDecisionRequest;
import com.healthsuite.marketplace.dto.response.AdminMarketplaceStatsResponse;
import com.healthsuite.marketplace.dto.response.DoctorDocumentResponse;
import com.healthsuite.marketplace.dto.response.DoctorProfileResponse;
import com.healthsuite.marketplace.entity.DoctorDocument;
import com.healthsuite.marketplace.enums.DoctorStatus;
import com.healthsuite.marketplace.service.MarketplaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/admin/marketplace")
@RequiredArgsConstructor
@Tag(name = "Admin — Marketplace", description = "Admin operations for doctor approval and marketplace management")
public class AdminMarketplaceController {

    private final MarketplaceService marketplaceService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AdminMarketplaceStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.adminGetStats()));
    }

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

    @GetMapping("/doctors/{id}/documents")
    public ResponseEntity<ApiResponse<List<DoctorDocumentResponse>>> listDoctorDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(marketplaceService.adminListDoctorDocuments(id)));
    }

    @GetMapping("/doctors/{id}/documents/{docId}/download")
    public ResponseEntity<Resource> downloadDoctorDocument(
            @PathVariable Long id,
            @PathVariable Long docId) throws IOException {
        DoctorDocument doc = marketplaceService.adminGetDoctorDocument(id, docId);

        // headshots live in public storage; redirect to their served URL
        if (doc.getFilePath().startsWith("/files/")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(doc.getFilePath()))
                .build();
        }

        Path path = Paths.get(doc.getFilePath());
        if (!Files.exists(path)) {
            throw new ResourceNotFoundException("Document file missing on disk", docId);
        }
        String contentType = Files.probeContentType(path);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(
                contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
            .body(new FileSystemResource(path));
    }
}
