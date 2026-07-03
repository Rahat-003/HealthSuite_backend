package com.healthsuite.marketplace.dto.response;

import com.healthsuite.marketplace.entity.Prescription;

import java.time.LocalDateTime;
import java.util.List;

public record PrescriptionResponse(
    Long id,
    Long consultationId,
    Long doctorProfileId,
    String doctorName,
    String doctorSpecialty,
    String doctorQualifications,
    Long patientId,
    String diagnosis,
    String remarks,
    Integer followUpDays,
    List<MedicineItem> medicines,
    List<TestItem> tests,
    List<FileItem> files,
    LocalDateTime createdAt
) {
    public record MedicineItem(Long id, String name, String dosage, String frequency, String duration, String instructions) {}
    public record TestItem(Long id, String name, String note) {}
    public record FileItem(Long id, String fileName, String contentType, Long sizeBytes) {}

    public static PrescriptionResponse from(Prescription p) {
        return new PrescriptionResponse(
            p.getId(),
            p.getConsultation().getId(),
            p.getDoctorProfile().getId(),
            p.getDoctorProfile().getFullName(),
            p.getDoctorProfile().getSpecialty(),
            p.getDoctorProfile().getQualifications(),
            p.getPatientId(),
            p.getDiagnosis(),
            p.getRemarks(),
            p.getFollowUpDays(),
            p.getMedicines().stream()
                .map(m -> new MedicineItem(m.getId(), m.getName(), m.getDosage(), m.getFrequency(), m.getDuration(), m.getInstructions()))
                .toList(),
            p.getTests().stream()
                .map(t -> new TestItem(t.getId(), t.getName(), t.getNote()))
                .toList(),
            p.getFiles().stream()
                .map(f -> new FileItem(f.getId(), f.getFileName(), f.getContentType(), f.getSizeBytes()))
                .toList(),
            p.getCreatedAt()
        );
    }
}
