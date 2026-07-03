package com.healthsuite.marketplace.service;

import com.healthsuite.common.exception.BadRequestException;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.exception.UnauthorizedException;
import com.healthsuite.marketplace.entity.ConsultationRating;
import com.healthsuite.marketplace.entity.ConsultationRequest;
import com.healthsuite.marketplace.entity.DoctorProfile;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import com.healthsuite.marketplace.enums.OfferStatus;
import com.healthsuite.marketplace.repository.ConsultationRatingRepository;
import com.healthsuite.marketplace.repository.ConsultationRequestRepository;
import com.healthsuite.marketplace.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final ConsultationRatingRepository ratingRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final DoctorProfileRepository doctorProfileRepository;

    public record RatingView(Integer stars, String comment, LocalDateTime createdAt) {}

    /** Patient rates a COMPLETED consultation, once; doctor aggregates update atomically. */
    @Transactional
    public RatingView rate(Long consultationId, Long patientId, int stars, String comment) {
        if (stars < 1 || stars > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars.");
        }
        ConsultationRequest consultation = consultationRequestRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("Consultation", consultationId));
        if (!consultation.getPatientId().equals(patientId)) {
            throw new UnauthorizedException("Only the patient can rate this consultation.");
        }
        if (consultation.getStatus() != ConsultationStatus.COMPLETED) {
            throw new ConflictException("You can rate a consultation once it is completed.");
        }
        if (ratingRepository.existsByConsultationId(consultationId)) {
            throw new ConflictException("You have already rated this consultation.");
        }

        DoctorProfile doctor = consultation.getOffers().stream()
            .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
            .map(o -> o.getDoctorProfile())
            .findFirst()
            .orElseThrow(() -> new ConflictException("No accepting doctor found for this consultation."));

        String trimmed = comment == null ? null : comment.trim();
        ConsultationRating rating = ratingRepository.save(ConsultationRating.builder()
            .consultation(consultation)
            .doctorProfile(doctor)
            .patientId(patientId)
            .stars(stars)
            .comment(trimmed == null || trimmed.isEmpty() ? null : trimmed)
            .build());

        doctor.setRating(ratingRepository.averageForDoctor(doctor.getId()).setScale(2, RoundingMode.HALF_UP));
        doctor.setRatingCount((int) ratingRepository.countByDoctorProfileId(doctor.getId()));
        doctorProfileRepository.save(doctor);

        log.info("Patient {} rated consultation {} with {} stars", patientId, consultationId, stars);
        return new RatingView(rating.getStars(), rating.getComment(), rating.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public Optional<RatingView> get(Long consultationId) {
        return ratingRepository.findByConsultationId(consultationId)
            .map(r -> new RatingView(r.getStars(), r.getComment(), r.getCreatedAt()));
    }
}
