package com.healthsuite.marketplace.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.marketplace.entity.ConsultationRequest;
import com.healthsuite.marketplace.entity.DoctorProfile;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import com.healthsuite.marketplace.enums.OfferStatus;
import com.healthsuite.marketplace.repository.ConsultationOfferRepository;
import com.healthsuite.marketplace.repository.DoctorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Doctor's patient roster, derived from their accepted consultations. */
@Service
@RequiredArgsConstructor
public class DoctorPatientsService {

    private final ConsultationOfferRepository consultationOfferRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;

    public record PatientSummary(
        Long patientId,
        String patientName,
        int consultationCount,
        int completedCount,
        LocalDateTime lastConsultationAt,
        Long lastCompletedConsultationId
    ) {}

    @Transactional(readOnly = true)
    public List<PatientSummary> getMyPatients(Long doctorUserId) {
        DoctorProfile profile = doctorProfileRepository.findByUserId(doctorUserId)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile for user", doctorUserId));

        Map<Long, List<ConsultationRequest>> byPatient = new LinkedHashMap<>();
        consultationOfferRepository.findByDoctorProfileIdAndStatus(profile.getId(), OfferStatus.ACCEPTED)
            .stream()
            .map(o -> o.getRequest())
            .sorted(Comparator.comparing(ConsultationRequest::getCreatedAt).reversed())
            .forEach(r -> byPatient.computeIfAbsent(r.getPatientId(), k -> new ArrayList<>()).add(r));

        return byPatient.entrySet().stream().map(e -> {
            List<ConsultationRequest> consults = e.getValue();
            String name = userRepository.findById(e.getKey()).map(User::getFullName).orElse("Patient");
            Long lastCompletedId = consults.stream()
                .filter(c -> c.getStatus() == ConsultationStatus.COMPLETED)
                .map(ConsultationRequest::getId)
                .findFirst().orElse(null);
            return new PatientSummary(
                e.getKey(),
                name,
                consults.size(),
                (int) consults.stream().filter(c -> c.getStatus() == ConsultationStatus.COMPLETED).count(),
                consults.get(0).getCreatedAt(),
                lastCompletedId
            );
        }).toList();
    }
}
