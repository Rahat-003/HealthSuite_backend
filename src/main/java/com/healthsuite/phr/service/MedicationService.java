package com.healthsuite.phr.service;

import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.phr.dto.request.AddMedicationRequest;
import com.healthsuite.phr.dto.request.UpdateMedicationRequest;
import com.healthsuite.phr.dto.response.MedicationResponse;
import com.healthsuite.phr.entity.Medication;
import com.healthsuite.phr.entity.MedicalVisit;
import com.healthsuite.phr.repository.MedicalVisitRepository;
import com.healthsuite.phr.repository.MedicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicationService {

    private final MedicationRepository medicationRepository;
    private final MedicalVisitRepository visitRepository;

    @Transactional
    public MedicationResponse addMedication(Long visitId, Long userId, AddMedicationRequest request) {
        MedicalVisit visit = visitRepository.findByIdAndUserId(visitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalVisit", visitId));

        Medication medication = Medication.builder()
                .visit(visit)
                .drugName(request.drugName())
                .dosagePattern(request.dosagePattern())
                .durationDays(request.durationDays())
                .mealTiming(request.mealTiming())
                .notes(request.notes())
                .build();

        return MedicationResponse.from(medicationRepository.save(medication));
    }

    @Transactional(readOnly = true)
    public List<MedicationResponse> getMedications(Long visitId, Long userId) {
        visitRepository.findByIdAndUserId(visitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalVisit", visitId));
        return medicationRepository.findByVisitId(visitId).stream()
                .map(MedicationResponse::from).toList();
    }

    @Transactional
    public MedicationResponse updateMedication(Long visitId, Long medicationId, Long userId,
                                               UpdateMedicationRequest request) {
        visitRepository.findByIdAndUserId(visitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalVisit", visitId));
        Medication medication = medicationRepository.findByIdAndVisitId(medicationId, visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", medicationId));

        medication.setDrugName(request.drugName());
        medication.setDosagePattern(request.dosagePattern());
        medication.setDurationDays(request.durationDays());
        medication.setMealTiming(request.mealTiming());
        medication.setNotes(request.notes());

        return MedicationResponse.from(medicationRepository.save(medication));
    }

    @Transactional
    public void deleteMedication(Long visitId, Long medicationId, Long userId) {
        visitRepository.findByIdAndUserId(visitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalVisit", visitId));
        Medication medication = medicationRepository.findByIdAndVisitId(medicationId, visitId)
                .orElseThrow(() -> new ResourceNotFoundException("Medication", medicationId));
        medicationRepository.delete(medication);
    }
}
