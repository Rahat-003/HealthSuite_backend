package com.healthsuite.marketplace.entity;

import com.healthsuite.common.entity.BaseEntity;
import com.healthsuite.marketplace.enums.DoctorStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "marketplace_doctor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String specialty;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String qualifications;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @Column(name = "license_number", nullable = false, unique = true, length = 100)
    private String licenseNumber;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "hospital_affiliation", length = 255)
    private String hospitalAffiliation;

    @Column(name = "availability_note", length = 255)
    private String availabilityNote;

    @Column(name = "consultation_fee_bdt", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal consultationFeeBdt = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DoctorStatus status = DoctorStatus.PENDING;

    @Column(name = "profile_photo_url", length = 1000)
    private String profilePhotoUrl;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private boolean isAvailable = true;

    @Column(nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "total_consultations", nullable = false)
    @Builder.Default
    private Integer totalConsultations = 0;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
