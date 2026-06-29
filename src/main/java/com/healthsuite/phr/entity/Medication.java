package com.healthsuite.phr.entity;

import com.healthsuite.phr.enums.MealTiming;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "medications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private MedicalVisit visit;

    @Column(name = "drug_name", nullable = false, length = 255)
    private String drugName;

    // Examples: "1+0+1" (morning + night), "1+1+1" (3x daily), "10mg"
    @Column(name = "dosage_pattern", nullable = false, length = 50)
    private String dosagePattern;

    @Column(name = "duration_days")
    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_timing", nullable = false, length = 20)
    private MealTiming mealTiming;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
