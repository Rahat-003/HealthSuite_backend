package com.healthsuite.family.dto.response;

import com.healthsuite.family.entity.FamilyMember;
import com.healthsuite.family.enums.FamilyRelationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

public record FamilyMemberResponse(
        Long id,
        Long ownerUserId,
        Long memberUserId,
        String memberFullName,
        String memberPhoneNumber,
        FamilyRelationStatus status,
        LocalDateTime addedAt,
        Instant verifiedAt
) {
    public static FamilyMemberResponse from(FamilyMember fm) {
        return new FamilyMemberResponse(
                fm.getId(),
                fm.getOwner().getId(),
                fm.getMember().getId(),
                fm.getMember().getFullName(),
                fm.getMember().getPhoneNumber(),
                fm.getStatus(),
                fm.getCreatedAt(),
                fm.getVerifiedAt()
        );
    }
}
