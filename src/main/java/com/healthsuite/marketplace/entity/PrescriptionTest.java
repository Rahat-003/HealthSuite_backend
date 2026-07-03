package com.healthsuite.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "marketplace_prescription_tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prescription_id")
    private Prescription prescription;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 300)
    private String note;
}
