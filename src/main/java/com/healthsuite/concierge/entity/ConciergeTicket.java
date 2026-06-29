package com.healthsuite.concierge.entity;

import com.healthsuite.auth.entity.User;
import com.healthsuite.common.entity.BaseEntity;
import com.healthsuite.concierge.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "concierge_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConciergeTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The patient this appointment is for
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    // Who submitted the ticket (may be a family proxy, may equal owner)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdByUser;

    // ROLE_SUPPORT agent assigned on PROCESSING transition
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_support_id")
    private User assignedSupport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.PENDING;

    @Column(name = "doctor_name", nullable = false, length = 255)
    private String doctorName;

    @Column(name = "doctor_specialty", length = 255)
    private String doctorSpecialty;

    @Column(name = "chamber_address", nullable = false, columnDefinition = "TEXT")
    private String chamberAddress;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Set when CONFIRMED
    @Column(name = "serial_number", length = 50)
    private String serialNumber;

    @Column(name = "estimated_arrival_time")
    private LocalDateTime estimatedArrivalTime;

    // Required when CANCELLED
    @Column(name = "cancellation_notes", columnDefinition = "TEXT")
    private String cancellationNotes;

    // Optimistic lock — prevents concurrent state mutations
    @Version
    private Long version;
}
