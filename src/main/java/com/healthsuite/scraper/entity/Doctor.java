package com.healthsuite.scraper.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bmdc_number", unique = true, length = 50)
    private String bmdcNumber;

    @Column(name = "full_name", nullable = false, length = 255)
    private String fullName;

    @Column(length = 255)
    private String specialty;

    @Column(name = "chamber_address", columnDefinition = "TEXT")
    private String chamberAddress;

    @Column(name = "hospital_name", length = 255)
    private String hospitalName;

    @Column(name = "consultation_fee", precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "available_slots", columnDefinition = "TEXT")
    private String availableSlots;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "profile_url", length = 1000)
    private String profileUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "last_scraped_at")
    private Instant lastScrapedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    private Instant updatedAt;
}
