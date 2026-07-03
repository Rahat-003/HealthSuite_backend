package com.healthsuite.marketplace.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.marketplace.dto.request.PrescriptionRequest;
import com.healthsuite.marketplace.dto.response.ConsultationRequestResponse;
import com.healthsuite.marketplace.dto.response.PrescriptionResponse;
import com.healthsuite.marketplace.entity.PrescriptionFile;
import com.healthsuite.marketplace.service.ConsultationRoomService;
import com.healthsuite.marketplace.service.PrescriptionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Prescriptions", description = "Consultation prescriptions: doctor submits, both participants read")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;
    private final ConsultationRoomService consultationRoomService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    /**
     * Doctor submits the prescription (multipart): `payload` is the JSON body,
     * `files` are optional prescription copies (image/PDF). Completes the case.
     */
    @SneakyThrows
    @PostMapping(value = "/api/doctor/consultations/{id}/prescription", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PrescriptionResponse>> submit(
            @PathVariable Long id,
            @RequestPart("payload") String payload,
            @RequestPart(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal) {

        PrescriptionRequest req = objectMapper.readValue(payload, PrescriptionRequest.class);
        var violations = validator.validate(req);
        if (!violations.isEmpty()) {
            throw new com.healthsuite.common.exception.BadRequestException(
                violations.iterator().next().getPropertyPath() + " " + violations.iterator().next().getMessage());
        }
        return ResponseEntity.ok(ApiResponse.ok("Prescription submitted — consultation completed.",
            prescriptionService.submit(id, principal.getId(), req, files)));
    }

    @GetMapping("/api/marketplace/consultations/{id}/prescription")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> get(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(prescriptionService.get(id, principal.getId())));
    }

    @GetMapping("/api/marketplace/consultations/{id}/prescription/files/{fileId}")
    public ResponseEntity<FileSystemResource> downloadFile(
            @PathVariable Long id,
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserPrincipal principal) {
        PrescriptionFile file = prescriptionService.getFileForDownload(id, fileId, principal.getId());
        MediaType mediaType = file.getContentType() != null
            ? MediaType.parseMediaType(file.getContentType())
            : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
            .contentType(mediaType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getFileName() + "\"")
            .body(new FileSystemResource(file.getFilePath()));
    }

    /** Room context for either participant (patient or accepting doctor). */
    @GetMapping("/api/marketplace/consultations/{id}/room")
    public ResponseEntity<ApiResponse<ConsultationRequestResponse>> roomView(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
            consultationRoomService.getParticipantView(id, principal.getId())));
    }
}
