package com.healthsuite.phr.dto.response;

import com.healthsuite.phr.entity.MedicalVisit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MedicalVisitResponse(
        Long id,
        Long userId,
        LocalDate visitDate,
        String doctorName,
        String doctorSpecialty,
        String hospitalName,
        String notes,
        List<DiagnosisResponse> diagnoses,
        List<MedicationResponse> medications,
        List<VisitDocumentResponse> documents,
        LocalDateTime createdAt
) {
    public static MedicalVisitResponse from(MedicalVisit visit) {
        return new MedicalVisitResponse(
                visit.getId(),
                visit.getUserId(),
                visit.getVisitDate(),
                visit.getDoctorName(),
                visit.getDoctorSpecialty(),
                visit.getHospitalName(),
                visit.getNotes(),
                visit.getDiagnoses().stream().map(DiagnosisResponse::from).toList(),
                visit.getMedications().stream().map(MedicationResponse::from).toList(),
                visit.getDocuments().stream().map(VisitDocumentResponse::from).toList(),
                visit.getCreatedAt()
        );
    }
}
