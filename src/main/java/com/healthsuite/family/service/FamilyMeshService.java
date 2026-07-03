package com.healthsuite.family.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.family.dto.response.FamilyMemberResponse;
import com.healthsuite.family.entity.FamilyMember;
import com.healthsuite.family.entity.FamilyRecordShare;
import com.healthsuite.family.enums.FamilyRelationStatus;
import com.healthsuite.family.enums.ShareScope;
import com.healthsuite.family.repository.FamilyMemberRepository;
import com.healthsuite.family.repository.FamilyRecordShareRepository;
import com.healthsuite.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FamilyMeshService {

    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyRecordShareRepository shareRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * Central authorization check for proxy access (concierge booking etc.).
     * Returns true when a VERIFIED relationship exists in either direction.
     * NOTE: health-record reads additionally require an explicit share grant —
     * see FamilyRecordShareService.
     */
    @Transactional(readOnly = true)
    public boolean canAccess(Long requesterId, Long targetOwnerId) {
        if (requesterId.equals(targetOwnerId)) return true;
        return familyMemberRepository.existsVerifiedRelationship(requesterId, targetOwnerId);
    }

    @Transactional
    public FamilyMemberResponse addFamilyMember(String targetPhone, String relationship, Long currentUserId) {
        User target = userRepository.findByPhoneNumber(targetPhone)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with phone number: " + targetPhone));

        if (target.getId().equals(currentUserId)) {
            throw new ConflictException("You cannot add yourself as a family member");
        }
        if (familyMemberRepository.existsByOwnerIdAndMemberId(currentUserId, target.getId())
                || familyMemberRepository.existsByOwnerIdAndMemberId(target.getId(), currentUserId)) {
            throw new ConflictException("A family link with this person already exists");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        FamilyMember tie = FamilyMember.builder()
                .owner(owner)
                .member(target)
                .relationship(relationship)
                .status(FamilyRelationStatus.PENDING_VERIFICATION)
                .build();
        FamilyMember saved = familyMemberRepository.save(tie);

        notificationService.notify(target.getId(), "FAMILY_REQUEST",
                "Family link request",
                owner.getFullName() + " wants to add you as family"
                        + (relationship != null ? " (" + labelOf(relationship) + ")" : "")
                        + ". Accept to connect.",
                "/family");

        return FamilyMemberResponse.from(saved);
    }

    @Transactional
    public FamilyMemberResponse acceptRequest(Long tieId, Long confirmingUserId) {
        FamilyMember tie = familyMemberRepository.findByIdAndMemberId(tieId, confirmingUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Family request not found or you are not the recipient"));

        if (tie.getStatus() == FamilyRelationStatus.VERIFIED) {
            throw new ConflictException("Family relationship is already verified");
        }

        tie.setStatus(FamilyRelationStatus.VERIFIED);
        tie.setVerifiedAt(Instant.now());
        FamilyMember saved = familyMemberRepository.save(tie);

        notificationService.notify(tie.getOwner().getId(), "FAMILY_ACCEPTED",
                "Family link accepted",
                tie.getMember().getFullName() + " accepted your family request. "
                        + "You can now choose which health records to share.",
                "/family");

        return FamilyMemberResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getMyFamilyMembers(Long userId) {
        // Resolve my grants and their grants once, then decorate each tie per-viewer
        Map<Long, FamilyRecordShare> myGrants = shareRepository.findAllByGrantorUserId(userId).stream()
                .collect(Collectors.toMap(FamilyRecordShare::getGranteeUserId, Function.identity()));
        Map<Long, FamilyRecordShare> theirGrants = shareRepository.findAllByGranteeUserId(userId).stream()
                .collect(Collectors.toMap(FamilyRecordShare::getGrantorUserId, Function.identity()));

        return familyMemberRepository.findAllByUserId(userId).stream()
                .map(fm -> {
                    Long otherId = fm.getOwner().getId().equals(userId)
                            ? fm.getMember().getId() : fm.getOwner().getId();
                    FamilyRecordShare mine = myGrants.get(otherId);
                    FamilyRecordShare theirs = theirGrants.get(otherId);
                    return FamilyMemberResponse.from(fm,
                            mine == null ? null : mine.getScope().name(),
                            mine == null || mine.getScope() == ShareScope.ALL ? null : mine.getVisitIds().size(),
                            theirs == null ? null : theirs.getScope().name(),
                            theirs == null || theirs.getScope() == ShareScope.ALL ? null : theirs.getVisitIds().size());
                })
                .toList();
    }

    @Transactional
    public void removeFamilyMember(Long tieId, Long currentUserId) {
        FamilyMember tie = familyMemberRepository.findById(tieId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", tieId));

        Long ownerId = tie.getOwner().getId();
        Long memberId = tie.getMember().getId();
        boolean isParticipant = ownerId.equals(currentUserId) || memberId.equals(currentUserId);
        if (!isParticipant) {
            throw new ResourceNotFoundException("FamilyMember", tieId);
        }

        // Unlinking withdraws record access in both directions
        shareRepository.findByGrantorUserIdAndGranteeUserId(ownerId, memberId)
                .ifPresent(shareRepository::delete);
        shareRepository.findByGrantorUserIdAndGranteeUserId(memberId, ownerId)
                .ifPresent(shareRepository::delete);

        familyMemberRepository.delete(tie);
    }

    private static String labelOf(String relationship) {
        return relationship.charAt(0) + relationship.substring(1).toLowerCase();
    }
}
