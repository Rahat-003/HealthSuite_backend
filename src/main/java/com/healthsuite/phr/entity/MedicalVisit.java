package com.healthsuite.phr.entity;

import com.healthsuite.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "medical_visits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalVisit extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "visit_date", nullable = false)
    private LocalDate visitDate;

    @Column(name = "doctor_name", nullable = false, length = 255)
    private String doctorName;

    @Column(name = "doctor_specialty", length = 255)
    private String doctorSpecialty;

    @Column(name = "hospital_name", length = 255)
    private String hospitalName;

    @Column(columnDefinition = "TEXT")
    private String notes;

    /** Set when this visit was auto-filed from a marketplace consultation. */
    @Column(name = "consultation_id")
    private Long consultationId;

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Diagnosis> diagnoses = new ArrayList<>();

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Medication> medications = new ArrayList<>();

    @OneToMany(mappedBy = "visit", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VisitDocument> documents = new ArrayList<>();
}
