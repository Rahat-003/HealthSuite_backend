package com.healthsuite.concierge.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateTicketRequest(
        // If null, the ticket is for the requester themselves; otherwise it's a proxy booking
        Long targetPatientId,
        @NotBlank String doctorName,
        String doctorSpecialty,
        @NotBlank String chamberAddress,
        String description
) {}
