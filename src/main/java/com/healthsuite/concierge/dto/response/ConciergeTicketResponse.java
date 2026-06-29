package com.healthsuite.concierge.dto.response;

import com.healthsuite.concierge.entity.ConciergeTicket;
import com.healthsuite.concierge.enums.TicketStatus;

import java.time.LocalDateTime;

public record ConciergeTicketResponse(
        Long id,
        Long ownerId,
        String ownerName,
        Long createdByUserId,
        String createdByUserName,
        Long assignedSupportId,
        String assignedSupportName,
        TicketStatus status,
        String doctorName,
        String doctorSpecialty,
        String chamberAddress,
        String description,
        String serialNumber,
        LocalDateTime estimatedArrivalTime,
        String cancellationNotes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ConciergeTicketResponse from(ConciergeTicket t) {
        return new ConciergeTicketResponse(
                t.getId(),
                t.getOwner().getId(),
                t.getOwner().getFullName(),
                t.getCreatedByUser().getId(),
                t.getCreatedByUser().getFullName(),
                t.getAssignedSupport() != null ? t.getAssignedSupport().getId() : null,
                t.getAssignedSupport() != null ? t.getAssignedSupport().getFullName() : null,
                t.getStatus(),
                t.getDoctorName(),
                t.getDoctorSpecialty(),
                t.getChamberAddress(),
                t.getDescription(),
                t.getSerialNumber(),
                t.getEstimatedArrivalTime(),
                t.getCancellationNotes(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
