package com.healthsuite.concierge.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelTicketRequest(
        @NotBlank @Size(min = 10, message = "Cancellation notes must be at least 10 characters")
        String cancellationNotes
) {}
