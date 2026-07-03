package com.healthsuite.family.entity;

import com.healthsuite.auth.entity.User;
import com.healthsuite.common.entity.BaseEntity;
import com.healthsuite.family.enums.FamilyRelationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "family_members",
        uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "member_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_user_id", nullable = false)
    private User member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private FamilyRelationStatus status = FamilyRelationStatus.PENDING_VERIFICATION;

    /** How the member relates to the owner, as declared by the owner (e.g. MOTHER). */
    @Column(length = 30)
    private String relationship;

    @Column(name = "verified_at")
    private Instant verifiedAt;
}
