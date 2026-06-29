package com.healthsuite.phr.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private MedicalVisit visit;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 20)
    @Builder.Default
    private String type = "DISEASE";

    @Column(columnDefinition = "TEXT")
    private String notes;
}
