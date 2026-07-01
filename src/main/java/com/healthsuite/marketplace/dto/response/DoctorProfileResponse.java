package com.healthsuite.marketplace.dto.response;

import com.healthsuite.marketplace.entity.DoctorProfile;
import com.healthsuite.marketplace.enums.DoctorStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DoctorProfileResponse(
    Long id,
    Long userId,
    String fullName,
    String specialty,
    String qualifications,
    int experienceYears,
    String licenseNumber,
    String bio,
    String hospitalAffiliation,
    String availabilityNote,
    BigDecimal consultationFeeBdt,
    DoctorStatus status,
    String profilePhotoUrl,
    BigDecimal rating,
    int totalConsultations,
    LocalDateTime createdAt
) {
    public static DoctorProfileResponse from(DoctorProfile d) {
        return new DoctorProfileResponse(
            d.getId(), d.getUserId(), d.getFullName(), d.getSpecialty(),
            d.getQualifications(), d.getExperienceYears(), d.getLicenseNumber(),
            d.getBio(), d.getHospitalAffiliation(), d.getAvailabilityNote(),
            d.getConsultationFeeBdt(), d.getStatus(), d.getProfilePhotoUrl(),
            d.getRating(), d.getTotalConsultations(), d.getCreatedAt()
        );
    }
}
