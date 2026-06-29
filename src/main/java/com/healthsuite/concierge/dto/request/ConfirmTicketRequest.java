package com.healthsuite.concierge.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ConfirmTicketRequest(
        @NotBlank String serialNumber,
        @NotNull @Future LocalDateTime estimatedArrivalTime
) {}
