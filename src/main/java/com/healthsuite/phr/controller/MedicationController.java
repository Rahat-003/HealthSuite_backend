package com.healthsuite.phr.controller;

import com.healthsuite.auth.security.UserPrincipal;
import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.phr.dto.request.AddMedicationRequest;
import com.healthsuite.phr.dto.response.MedicationResponse;
import com.healthsuite.phr.service.MedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/phr/visits/{visitId}/medications")
@RequiredArgsConstructor
@Tag(name = "Personal Health Records (PHR)")
public class MedicationController {

    private final MedicationService medicationService;

    @Operation(summary = "Add a medication to a visit",
            description = "Dosage pattern examples: `1+0+1` (morning+afternoon+night), `10mg`. Pattern: `^[\\d+]+(?:\\+[\\d+]+)*$|^[\\d.]+\\s*\\w+.*$`")
    @PostMapping
    public ResponseEntity<ApiResponse<MedicationResponse>> addMedication(
            @PathVariable Long visitId,
            @Valid @RequestBody AddMedicationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(medicationService.addMedication(visitId, principal.getId(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicationResponse>>> getMedications(
            @PathVariable Long visitId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(medicationService.getMedications(visitId, principal.getId())));
    }

    @DeleteMapping("/{medicationId}")
    public ResponseEntity<ApiResponse<Void>> deleteMedication(
            @PathVariable Long visitId,
            @PathVariable Long medicationId,
            @AuthenticationPrincipal UserPrincipal principal) {
        medicationService.deleteMedication(visitId, medicationId, principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Medication removed"));
    }
}
