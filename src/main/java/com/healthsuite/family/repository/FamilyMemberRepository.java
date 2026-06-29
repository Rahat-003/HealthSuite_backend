package com.healthsuite.family.repository;

import com.healthsuite.family.entity.FamilyMember;
import com.healthsuite.family.enums.FamilyRelationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    /**
     * Bidirectional verified relationship check:
     * returns true if userA and userB have a VERIFIED tie in either direction.
     */
    @Query("""
            SELECT COUNT(fm) > 0 FROM FamilyMember fm
            WHERE fm.status = com.healthsuite.family.enums.FamilyRelationStatus.VERIFIED
              AND (
                  (fm.owner.id = :userIdA AND fm.member.id = :userIdB)
                  OR
                  (fm.owner.id = :userIdB AND fm.member.id = :userIdA)
              )
            """)
    boolean existsVerifiedRelationship(@Param("userIdA") Long userIdA,
                                       @Param("userIdB") Long userIdB);

    @Query("""
            SELECT fm FROM FamilyMember fm
            WHERE fm.owner.id = :userId OR fm.member.id = :userId
            """)
    List<FamilyMember> findAllByUserId(@Param("userId") Long userId);

    Optional<FamilyMember> findByOwnerIdAndMemberId(Long ownerId, Long memberId);

    // Used by the member to accept the request (member confirms their side)
    Optional<FamilyMember> findByIdAndMemberId(Long id, Long memberId);

    boolean existsByOwnerIdAndMemberId(Long ownerId, Long memberId);

    boolean existsByOwnerIdAndMemberIdAndStatus(Long ownerId, Long memberId, FamilyRelationStatus status);

    // Spring Data JPA derives these from the entity field names: owner.id → OwnerId, member.id → MemberId
}
