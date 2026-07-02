package com.healthsuite.marketplace.dto.response;

import com.healthsuite.marketplace.entity.ConsultationMedia;
import com.healthsuite.marketplace.entity.ConsultationRequest;
import com.healthsuite.marketplace.enums.ConsultationMediaType;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import com.healthsuite.marketplace.enums.OfferStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ConsultationRequestResponse(
    Long id,
    Long patientId,
    String patientName,
    String problemText,
    String audioUrl,
    int refundWindowHours,
    ConsultationStatus status,
    BigDecimal upfrontAmountBdt,
    BigDecimal postConsultAmountBdt,
    List<OfferSummary> offers,
    List<MediaSummary> mediaFiles,
    LocalDateTime createdAt,
    LocalDateTime expiresAt,
    LocalDateTime cancelAvailableAt,
    boolean canCancel
) {
    public record OfferSummary(
        Long id,
        Long doctorProfileId,
        String doctorName,
        OfferStatus status
    ) {}

    public record MediaSummary(
        Long id,
        String fileName,
        ConsultationMediaType mediaType,
        LocalDateTime createdAt
    ) {}

    public static ConsultationRequestResponse from(ConsultationRequest r) {
        return from(r, List.of(), null, null);
    }

    public static ConsultationRequestResponse from(ConsultationRequest r, List<ConsultationMedia> media) {
        return from(r, media, null, null);
    }

    public static ConsultationRequestResponse from(ConsultationRequest r, List<ConsultationMedia> media, String patientName) {
        return from(r, media, patientName, null);
    }

    /** cancelWaitHours is null when the caller doesn't need cancel-eligibility computed (e.g. doctor-side views). */
    public static ConsultationRequestResponse from(ConsultationRequest r, List<ConsultationMedia> media, String patientName, Integer cancelWaitHours) {
        var offers = r.getOffers().stream()
            .map(o -> new OfferSummary(
                o.getId(),
                o.getDoctorProfile().getId(),
                o.getDoctorProfile().getFullName(),
                o.getStatus()
            ))
            .toList();

        var mediaFiles = media.stream()
            .map(m -> new MediaSummary(m.getId(), m.getFileName(), m.getMediaType(), m.getCreatedAt()))
            .toList();

        boolean pending = r.getStatus() == ConsultationStatus.QUEUED;
        LocalDateTime cancelAvailableAt = pending && cancelWaitHours != null
            ? r.getCreatedAt().plusHours(cancelWaitHours)
            : null;
        boolean canCancel = pending && cancelAvailableAt != null && !LocalDateTime.now().isBefore(cancelAvailableAt);

        return new ConsultationRequestResponse(
            r.getId(), r.getPatientId(), patientName, r.getProblemText(), r.getAudioUrl(),
            r.getRefundWindowHours(), r.getStatus(),
            r.getUpfrontAmountBdt(), r.getPostConsultAmountBdt(),
            offers, mediaFiles, r.getCreatedAt(), r.getExpiresAt(),
            cancelAvailableAt, canCancel
        );
    }
}
