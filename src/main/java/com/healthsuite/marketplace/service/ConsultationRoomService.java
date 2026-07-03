package com.healthsuite.marketplace.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.exception.UnauthorizedException;
import com.healthsuite.marketplace.dto.response.ConsultationRequestResponse;
import com.healthsuite.marketplace.entity.ConsultationOffer;
import com.healthsuite.marketplace.entity.ConsultationRequest;
import com.healthsuite.marketplace.enums.OfferStatus;
import com.healthsuite.marketplace.repository.ConsultationMediaRepository;
import com.healthsuite.marketplace.repository.ConsultationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Access checks + participant view for the live consultation room.
 * A room exists for every consultation; joinable while the case is ACTIVE.
 */
@Service
@RequiredArgsConstructor
public class ConsultationRoomService {

    private final ConsultationRequestRepository consultationRequestRepository;
    private final ConsultationMediaRepository consultationMediaRepository;
    private final UserRepository userRepository;

    public enum Role { PATIENT, DOCTOR }

    /** Throws unless the user is the patient or the accepting doctor. */
    @Transactional(readOnly = true)
    public Role requireParticipant(Long consultationId, Long userId) {
        ConsultationRequest request = consultationRequestRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("Consultation", consultationId));

        if (request.getPatientId().equals(userId)) return Role.PATIENT;

        boolean isAcceptedDoctor = request.getOffers().stream()
            .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
            .map(ConsultationOffer::getDoctorProfile)
            .anyMatch(d -> d.getUserId().equals(userId));
        if (isAcceptedDoctor) return Role.DOCTOR;

        throw new UnauthorizedException("You are not a participant of this consultation.");
    }

    /** Consultation details for either participant (used by the room page). */
    @Transactional(readOnly = true)
    public ConsultationRequestResponse getParticipantView(Long consultationId, Long userId) {
        requireParticipant(consultationId, userId);
        ConsultationRequest request = consultationRequestRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("Consultation", consultationId));
        String patientName = userRepository.findById(request.getPatientId())
            .map(User::getFullName)
            .orElse("Patient");
        return ConsultationRequestResponse.from(
            request,
            consultationMediaRepository.findByConsultationId(consultationId),
            patientName
        );
    }
}
