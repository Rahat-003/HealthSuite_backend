package com.healthsuite.marketplace.service;

import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.BadRequestException;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.exception.UnauthorizedException;
import com.healthsuite.marketplace.dto.response.DoctorReviewsResponse;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    private final ConsultationRatingRepository ratingRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

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

    /** Review feed for a doctor's profile page: latest 50, patient names shortened. */
    @Transactional(readOnly = true)
    public DoctorReviewsResponse reviewsForDoctor(Long doctorProfileId) {
        DoctorProfile doctor = doctorProfileRepository.findById(doctorProfileId)
            .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorProfileId));

        List<ConsultationRating> ratings =
            ratingRepository.findTop50ByDoctorProfileIdOrderByCreatedAtDesc(doctorProfileId);

        Map<Long, String> namesById = userRepository.findAllById(
                ratings.stream().map(ConsultationRating::getPatientId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(u -> u.getId(), u -> shortName(u.getFullName())));

        Map<Integer, Long> breakdown = ratingRepository.starBreakdownForDoctor(doctorProfileId).stream()
            .collect(Collectors.toMap(row -> (Integer) row[0], row -> (Long) row[1]));

        return new DoctorReviewsResponse(
            doctor.getRating(),
            ratingRepository.countByDoctorProfileId(doctorProfileId),
            breakdown,
            ratings.stream()
                .map(r -> new DoctorReviewsResponse.ReviewItem(
                    r.getStars(), r.getComment(),
                    namesById.getOrDefault(r.getPatientId(), "A patient"),
                    r.getCreatedAt()))
                .toList());
    }

    /** "Rahat Akhter" → "Rahat A." — reviews shouldn't expose the patient's full identity. */
    private String shortName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "A patient";
        String[] parts = fullName.trim().split("\\s+");
        return parts.length == 1 ? parts[0] : parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
