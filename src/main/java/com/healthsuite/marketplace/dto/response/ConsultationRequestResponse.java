package com.healthsuite.marketplace.dto.response;

import com.healthsuite.marketplace.entity.ConsultationRequest;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import com.healthsuite.marketplace.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ConsultationRequestResponse(
    Long id,
    Long patientId,
    String problemText,
    String audioUrl,
    int refundWindowHours,
    ConsultationStatus status,
    BigDecimal upfrontAmountBdt,
    BigDecimal postConsultAmountBdt,
    List<OfferSummary> offers,
    LocalDateTime createdAt,
    LocalDateTime expiresAt
) {
    public record OfferSummary(
        Long id,
        Long doctorProfileId,
        String doctorName,
        OfferStatus status
    ) {}

    public static ConsultationRequestResponse from(ConsultationRequest r) {
        var offers = r.getOffers().stream()
            .map(o -> new OfferSummary(
                o.getId(),
                o.getDoctorProfile().getId(),
                o.getDoctorProfile().getFullName(),
                o.getStatus()
            ))
            .toList();

        return new ConsultationRequestResponse(
            r.getId(), r.getPatientId(), r.getProblemText(), r.getAudioUrl(),
            r.getRefundWindowHours(), r.getStatus(),
            r.getUpfrontAmountBdt(), r.getPostConsultAmountBdt(),
            offers, r.getCreatedAt(), r.getExpiresAt()
        );
    }
}
