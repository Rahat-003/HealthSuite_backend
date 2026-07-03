package com.healthsuite.marketplace.service;

import com.healthsuite.common.exception.BadRequestException;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.exception.UnauthorizedException;
import com.healthsuite.marketplace.dto.request.PrescriptionRequest;
import com.healthsuite.marketplace.dto.response.PrescriptionResponse;
import com.healthsuite.marketplace.entity.*;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import com.healthsuite.marketplace.enums.OfferStatus;
import com.healthsuite.marketplace.repository.ConsultationOfferRepository;
import com.healthsuite.marketplace.repository.ConsultationRequestRepository;
import com.healthsuite.marketplace.repository.DoctorProfileRepository;
import com.healthsuite.marketplace.repository.PrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final ConsultationRequestRepository consultationRequestRepository;
    private final ConsultationOfferRepository consultationOfferRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final HealthRecordStorageService healthRecordStorage;

    /**
     * Doctor submits the prescription for an ACTIVE consultation they accepted.
     * Submission is the completion event: the case flips to COMPLETED and the
     * held post-consult amount is considered released to the doctor.
     * One prescription per consultation; immutable once submitted.
     */
    @Transactional
    public PrescriptionResponse submit(Long consultationId, Long doctorUserId,
                                       PrescriptionRequest req, List<MultipartFile> files) {
        DoctorProfile doctor = doctorProfileRepository.findByUserId(doctorUserId)
            .orElseThrow(() -> new ResourceNotFoundException("DoctorProfile for user", doctorUserId));

        ConsultationRequest consultation = consultationRequestRepository.findById(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("Consultation", consultationId));

        ConsultationOffer offer = consultationOfferRepository
            .findByRequestIdAndDoctorProfileId(consultationId, doctor.getId())
            .orElseThrow(() -> new UnauthorizedException("You are not assigned to this consultation."));
        if (offer.getStatus() != OfferStatus.ACCEPTED) {
            throw new UnauthorizedException("You are not the accepting doctor for this consultation.");
        }
        if (consultation.getStatus() != ConsultationStatus.ACTIVE) {
            throw new ConflictException("This consultation is not active — a prescription can only be submitted for an active case.");
        }
        if (prescriptionRepository.existsByConsultationId(consultationId)) {
            throw new ConflictException("A prescription has already been submitted for this consultation.");
        }

        boolean hasMedicines = req.medicines() != null && !req.medicines().isEmpty();
        boolean hasTests     = req.tests() != null && !req.tests().isEmpty();
        boolean hasRemarks   = req.remarks() != null && !req.remarks().isBlank();
        if (!hasMedicines && !hasTests && !hasRemarks) {
            throw new BadRequestException("A prescription needs at least one medicine, test, or remark.");
        }

        Prescription prescription = Prescription.builder()
            .consultation(consultation)
            .doctorProfile(doctor)
            .patientId(consultation.getPatientId())
            .diagnosis(trimToNull(req.diagnosis()))
            .remarks(trimToNull(req.remarks()))
            .followUpDays(req.followUpDays())
            .build();

        if (hasMedicines) {
            req.medicines().forEach(m -> prescription.getMedicines().add(
                PrescriptionMedicine.builder()
                    .prescription(prescription)
                    .name(m.name().trim())
                    .dosage(trimToNull(m.dosage()))
                    .frequency(trimToNull(m.frequency()))
                    .duration(trimToNull(m.duration()))
                    .instructions(trimToNull(m.instructions()))
                    .build()));
        }
        if (hasTests) {
            req.tests().forEach(t -> prescription.getTests().add(
                PrescriptionTest.builder()
                    .prescription(prescription)
                    .name(t.name().trim())
                    .note(trimToNull(t.note()))
                    .build()));
        }

        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                var stored = healthRecordStorage.store(consultation.getPatientId(), file);
                prescription.getFiles().add(PrescriptionFile.builder()
                    .prescription(prescription)
                    .fileName(stored.fileName())
                    .filePath(stored.filePath())
                    .contentType(stored.contentType())
                    .sizeBytes(stored.sizeBytes())
                    .build());
            }
        }

        consultation.setStatus(ConsultationStatus.COMPLETED);
        consultation.setCompletedAt(LocalDateTime.now());
        consultationRequestRepository.save(consultation);

        Prescription saved = prescriptionRepository.save(prescription);
        log.info("Doctor {} submitted prescription {} for consultation {} — case completed, escrow released",
            doctor.getId(), saved.getId(), consultationId);
        return PrescriptionResponse.from(saved);
    }

    /** Patient of the consultation or its accepting doctor may read the prescription. */
    @Transactional(readOnly = true)
    public PrescriptionResponse get(Long consultationId, Long userId) {
        Prescription p = prescriptionRepository.findByConsultationId(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription for consultation", consultationId));
        requireParticipant(p, userId);
        return PrescriptionResponse.from(p);
    }

    /** Returns the file entity after verifying the caller is a participant. */
    @Transactional(readOnly = true)
    public PrescriptionFile getFileForDownload(Long consultationId, Long fileId, Long userId) {
        Prescription p = prescriptionRepository.findByConsultationId(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription for consultation", consultationId));
        requireParticipant(p, userId);
        return p.getFiles().stream()
            .filter(f -> f.getId().equals(fileId))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException("Prescription file", fileId));
    }

    private void requireParticipant(Prescription p, Long userId) {
        boolean isPatient = p.getPatientId().equals(userId);
        boolean isDoctor  = p.getDoctorProfile().getUserId().equals(userId);
        if (!isPatient && !isDoctor) {
            throw new UnauthorizedException("You do not have access to this prescription.");
        }
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
