package com.healthsuite.scraper.controller;

import com.healthsuite.common.response.ApiResponse;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.scraper.entity.Doctor;
import com.healthsuite.scraper.repository.DoctorRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@Tag(name = "Doctor Directory", description = "Public doctor search populated by the nightly scraper (runs at 02:00 daily)")
public class DoctorController {

    private final DoctorRepository doctorRepository;

    @Operation(summary = "Search doctors", description = "Filter by name or specialty (mutually exclusive; name takes priority). No auth required.")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<Doctor>>> searchDoctors(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("fullName").ascending());

        var result = StringUtils.hasText(name)
                ? doctorRepository.findByIsActiveTrueAndFullNameContainingIgnoreCase(name, pageable)
                : StringUtils.hasText(specialty)
                ? doctorRepository.findByIsActiveTrueAndSpecialtyContainingIgnoreCase(specialty, pageable)
                : doctorRepository.findByIsActiveTrue(pageable);

        return ResponseEntity.ok(ApiResponse.ok(PagedResponse.from(result)));
    }

    @Operation(summary = "Get a doctor by ID")
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Doctor>> getDoctor(@PathVariable Long id) {
        return doctorRepository.findById(id)
                .map(d -> ResponseEntity.ok(ApiResponse.ok(d)))
                .orElse(ResponseEntity.notFound().build());
    }
}
