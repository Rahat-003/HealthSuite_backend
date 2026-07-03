package com.healthsuite.family.dto.response;

import com.healthsuite.family.entity.FamilyMember;
import com.healthsuite.family.enums.FamilyRelationStatus;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Both sides of the tie are included so the client can render either
 * direction (the viewer may be the owner who sent the request, or the
 * member who received it). Share fields describe record-sharing grants
 * between the viewer and the other person, resolved per-viewer.
 */
public record FamilyMemberResponse(
        Long id,
        Long ownerUserId,
        String ownerFullName,
        String ownerPhoneNumber,
        String ownerPhotoUrl,
        Long memberUserId,
        String memberFullName,
        String memberPhoneNumber,
        String memberPhotoUrl,
        FamilyRelationStatus status,
        String relationship,
        LocalDateTime addedAt,
        Instant verifiedAt,
        String myShareScope,      // what I share with them: null | ALL | SELECTED
        Integer mySharedCount,    // number of visits when SELECTED
        String theirShareScope,   // what they share with me: null | ALL | SELECTED
        Integer theirSharedCount
) {
    public static FamilyMemberResponse from(FamilyMember fm) {
        return from(fm, null, null, null, null);
    }

    public static FamilyMemberResponse from(FamilyMember fm,
                                            String myShareScope, Integer mySharedCount,
                                            String theirShareScope, Integer theirSharedCount) {
        return new FamilyMemberResponse(
                fm.getId(),
                fm.getOwner().getId(),
                fm.getOwner().getFullName(),
                fm.getOwner().getPhoneNumber(),
                fm.getOwner().getProfilePhotoUrl(),
                fm.getMember().getId(),
                fm.getMember().getFullName(),
                fm.getMember().getPhoneNumber(),
                fm.getMember().getProfilePhotoUrl(),
                fm.getStatus(),
                fm.getRelationship(),
                fm.getCreatedAt(),
                fm.getVerifiedAt(),
                myShareScope, mySharedCount,
                theirShareScope, theirSharedCount
        );
    }
}
