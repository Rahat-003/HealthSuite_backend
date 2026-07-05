package com.healthsuite.marketplace.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.marketplace.dto.response.LiveRoomResponse;
import com.healthsuite.marketplace.entity.ConsultationOffer;
import com.healthsuite.marketplace.entity.ConsultationRequest;
import com.healthsuite.marketplace.entity.DoctorProfile;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import com.healthsuite.marketplace.enums.OfferStatus;
import com.healthsuite.marketplace.repository.ConsultationOfferRepository;
import com.healthsuite.marketplace.repository.ConsultationRequestRepository;
import com.healthsuite.marketplace.ws.ConsultSignalingHandler;
import com.healthsuite.marketplace.ws.ConsultSignalingHandler.PeerJoinedEvent;
import com.healthsuite.marketplace.service.ConsultationRoomService.Role;
import com.healthsuite.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bridges live room occupancy to the rest of the app: notifies the absent
 * participant when their counterpart enters the consultation room, and
 * answers the "is anyone waiting for me in a call right now?" poll that
 * drives the in-app join modal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RoomPresenceService {

    /** Don't re-notify the same seat's join more often than this. */
    private static final long NOTIFY_COOLDOWN_SECONDS = 120;

    private final ConsultSignalingHandler signalingHandler;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final ConsultationOfferRepository consultationOfferRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    private final Map<String, Instant> lastNotified = new ConcurrentHashMap<>();

    @EventListener
    @Transactional(readOnly = true)
    public void onPeerJoined(PeerJoinedEvent event) {
        String key = event.consultationId() + ":" + event.joinerRole();
        Instant last = lastNotified.get(key);
        if (last != null && last.plusSeconds(NOTIFY_COOLDOWN_SECONDS).isAfter(Instant.now())) return;

        ConsultationRequest request = consultationRequestRepository.findById(event.consultationId()).orElse(null);
        if (request == null || request.getStatus() != ConsultationStatus.ACTIVE) return;

        Optional<DoctorProfile> doctor = acceptedDoctor(request);
        if (doctor.isEmpty()) return;

        String link = "/consultation/" + request.getId() + "/room";
        if (event.joinerRole() == Role.DOCTOR) {
            notificationService.notify(request.getPatientId(), "CALL_PEER_JOINED",
                doctor.get().getFullName() + " is in your consultation room",
                "Your doctor has joined the video call for case #" + request.getId() + ". Join now to start.",
                link);
        } else {
            String patientName = userRepository.findById(request.getPatientId())
                .map(User::getFullName).orElse("Your patient");
            notificationService.notify(doctor.get().getUserId(), "CALL_PEER_JOINED",
                patientName + " has joined the video call",
                "Your patient is waiting in the consultation room for case #" + request.getId() + ".",
                link);
        }
        lastNotified.put(key, Instant.now());
        if (lastNotified.size() > 500) {
            lastNotified.entrySet().removeIf(e ->
                e.getValue().plusSeconds(NOTIFY_COOLDOWN_SECONDS).isBefore(Instant.now()));
        }
    }

    /**
     * Active consultations of this user where the other participant is
     * connected to the room right now.
     */
    @Transactional(readOnly = true)
    public List<LiveRoomResponse> liveRooms(Long userId) {
        if (!signalingHandler.anyRoomOccupied()) return List.of();
        List<LiveRoomResponse> result = new ArrayList<>();

        // As patient: is my doctor in the room?
        for (ConsultationRequest request :
                consultationRequestRepository.findByPatientIdAndStatus(userId, ConsultationStatus.ACTIVE)) {
            if (!signalingHandler.isSeatOccupied(request.getId(), Role.DOCTOR)) continue;
            acceptedDoctor(request).ifPresent(d -> result.add(new LiveRoomResponse(
                request.getId(), "DOCTOR", d.getFullName(), d.getProfilePhotoUrl(),
                d.getSpecialty(), d.getId())));
        }

        // As doctor: is my patient in the room?
        for (ConsultationOffer offer : consultationOfferRepository.findActiveAcceptedForDoctorUser(userId)) {
            ConsultationRequest request = offer.getRequest();
            if (!signalingHandler.isSeatOccupied(request.getId(), Role.PATIENT)) continue;
            User patient = userRepository.findById(request.getPatientId()).orElse(null);
            result.add(new LiveRoomResponse(
                request.getId(), "PATIENT",
                patient != null ? patient.getFullName() : "Patient",
                patient != null ? patient.getProfilePhotoUrl() : null,
                "Case #" + request.getId(), null));
        }
        return result;
    }

    private Optional<DoctorProfile> acceptedDoctor(ConsultationRequest request) {
        return request.getOffers().stream()
            .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
            .map(ConsultationOffer::getDoctorProfile)
            .findFirst();
    }
}
