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
import com.healthsuite.notification.NotificationService;
import com.healthsuite.phr.entity.Diagnosis;
import com.healthsuite.phr.entity.MedicalVisit;
import com.healthsuite.phr.entity.Medication;
import com.healthsuite.phr.enums.MealTiming;
import com.healthsuite.phr.repository.MedicalVisitRepository;
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
    private final MedicalVisitRepository medicalVisitRepository;
    private final NotificationService notificationService;
    private final RxPdfService rxPdfService;

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

        doctor.setTotalConsultations(doctor.getTotalConsultations() + 1);
        doctorProfileRepository.save(doctor);

        Prescription saved = prescriptionRepository.save(prescription);

        autoFileIntoHealthRecords(saved, doctor);
        notificationService.notify(
            consultation.getPatientId(), "PRESCRIPTION_READY",
            "Your prescription is ready",
            doctor.getFullName() + " completed your consultation and submitted a prescription.",
            "/consultations");
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

    /** Printable PDF of the prescription, for either participant. */
    @Transactional(readOnly = true)
    public byte[] pdf(Long consultationId, Long userId) {
        Prescription p = prescriptionRepository.findByConsultationId(consultationId)
            .orElseThrow(() -> new ResourceNotFoundException("Prescription for consultation", consultationId));
        requireParticipant(p, userId);
        return rxPdfService.generate(p);
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

    /**
     * Auto-file the consultation outcome into the patient's health records:
     * the PHR becomes self-populating instead of manual-entry only.
     */
    private void autoFileIntoHealthRecords(Prescription p, com.healthsuite.marketplace.entity.DoctorProfile doctor) {
        try {
            MedicalVisit visit = MedicalVisit.builder()
                .userId(p.getPatientId())
                .visitDate(java.time.LocalDate.now())
                .doctorName(doctor.getFullName())
                .doctorSpecialty(doctor.getSpecialty())
                .hospitalName("HealthSuite video consultation")
                .notes(buildVisitNotes(p))
                .build();

            if (p.getDiagnosis() != null) {
                visit.getDiagnoses().add(Diagnosis.builder()
                    .visit(visit).name(p.getDiagnosis()).type("DISEASE").build());
            }
            p.getMedicines().forEach(m -> {
                String pattern = joinNonBlank(" ", m.getDosage(), m.getFrequency());
                visit.getMedications().add(Medication.builder()
                    .visit(visit)
                    .drugName(m.getName())
                    .dosagePattern(pattern.isEmpty() ? "As directed" : truncate(pattern, 50))
                    .durationDays(parseDays(m.getDuration()))
                    .mealTiming(guessMealTiming(m.getInstructions()))
                    .notes(m.getInstructions())
                    .build());
            });

            medicalVisitRepository.save(visit);
            log.info("Auto-filed consultation {} prescription into PHR visit for patient {}",
                p.getConsultation().getId(), p.getPatientId());
        } catch (Exception e) {
            // never fail the prescription because record-keeping failed
            log.warn("Could not auto-file prescription {} into PHR: {}", p.getId(), e.getMessage());
        }
    }

    private String buildVisitNotes(Prescription p) {
        StringBuilder sb = new StringBuilder("Video consultation via HealthSuite marketplace.");
        if (p.getRemarks() != null) sb.append("\n").append(p.getRemarks());
        if (p.getFollowUpDays() != null) sb.append("\nFollow up in ").append(p.getFollowUpDays()).append(" days.");
        return sb.toString();
    }

    private Integer parseDays(String duration) {
        if (duration == null) return null;
        var m = java.util.regex.Pattern.compile("\\d+").matcher(duration);
        return m.find() ? Integer.parseInt(m.group()) : null;
    }

    private MealTiming guessMealTiming(String instructions) {
        return instructions != null && instructions.toLowerCase().contains("before")
            ? MealTiming.BEFORE_MEAL : MealTiming.AFTER_MEAL;
    }

    private String joinNonBlank(String sep, String... parts) {
        return java.util.Arrays.stream(parts)
            .filter(x -> x != null && !x.isBlank())
            .collect(java.util.stream.Collectors.joining(sep));
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    private String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
