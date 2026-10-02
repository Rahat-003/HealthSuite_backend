package com.healthsuite.marketplace.entity;

import com.healthsuite.common.entity.BaseEntity;
import com.healthsuite.marketplace.enums.ConsultationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "marketplace_consultation_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsultationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "problem_text", nullable = false, columnDefinition = "TEXT")
    private String problemText;

    @Column(name = "audio_url", length = 1000)
    private String audioUrl;

    @Column(name = "refund_window_hours", nullable = false)
    @Builder.Default
    private Integer refundWindowHours = 3;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ConsultationStatus status = ConsultationStatus.QUEUED;

    @Column(name = "upfront_amount_bdt", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal upfrontAmountBdt = new BigDecimal("50");

    @Column(name = "post_consult_amount_bdt", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal postConsultAmountBdt = new BigDecimal("250");

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConsultationOffer> offers = new ArrayList<>();
}
