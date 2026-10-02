package com.healthsuite.family.entity;

import com.healthsuite.family.enums.ShareScope;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * One user's decision about what a specific family member may see:
 * everything (ALL) or a hand-picked set of visits (SELECTED).
 * Absence of a grant means nothing is shared.
 */
@Entity
@Table(name = "family_record_shares",
        uniqueConstraints = @UniqueConstraint(columnNames = {"grantor_user_id", "grantee_user_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FamilyRecordShare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "grantor_user_id", nullable = false)
    private Long grantorUserId;

    @Column(name = "grantee_user_id", nullable = false)
    private Long granteeUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ShareScope scope;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "family_record_share_visits",
            joinColumns = @JoinColumn(name = "share_id"))
    @Column(name = "visit_id")
    @Builder.Default
    private Set<Long> visitIds = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
