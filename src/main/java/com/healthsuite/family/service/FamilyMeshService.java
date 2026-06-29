package com.healthsuite.family.service;

import com.healthsuite.auth.entity.User;
import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.ConflictException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.family.dto.response.FamilyMemberResponse;
import com.healthsuite.family.entity.FamilyMember;
import com.healthsuite.family.enums.FamilyRelationStatus;
import com.healthsuite.family.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FamilyMeshService {

    private final FamilyMemberRepository familyMemberRepository;
    private final UserRepository userRepository;

    /**
     * Central authorization check for proxy access.
     * Returns true when a VERIFIED relationship exists in either direction.
     */
    @Transactional(readOnly = true)
    public boolean canAccess(Long requesterId, Long targetOwnerId) {
        if (requesterId.equals(targetOwnerId)) return true;
        return familyMemberRepository.existsVerifiedRelationship(requesterId, targetOwnerId);
    }

    @Transactional
    public FamilyMemberResponse addFamilyMember(String targetPhone, Long currentUserId) {
        User target = userRepository.findByPhoneNumber(targetPhone)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with phone number: " + targetPhone));

        if (target.getId().equals(currentUserId)) {
            throw new ConflictException("You cannot add yourself as a family member");
        }
        if (familyMemberRepository.existsByOwnerIdAndMemberId(currentUserId, target.getId())) {
            throw new ConflictException("Family member request already exists");
        }

        User owner = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUserId));

        FamilyMember tie = FamilyMember.builder()
                .owner(owner)
                .member(target)
                .status(FamilyRelationStatus.PENDING_VERIFICATION)
                .build();

        return FamilyMemberResponse.from(familyMemberRepository.save(tie));
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
        return FamilyMemberResponse.from(familyMemberRepository.save(tie));
    }

    @Transactional(readOnly = true)
    public List<FamilyMemberResponse> getMyFamilyMembers(Long userId) {
        return familyMemberRepository.findAllByUserId(userId).stream()
                .map(FamilyMemberResponse::from)
                .toList();
    }

    @Transactional
    public void removeFamilyMember(Long tieId, Long currentUserId) {
        FamilyMember tie = familyMemberRepository.findById(tieId)
                .orElseThrow(() -> new ResourceNotFoundException("FamilyMember", tieId));

        boolean isParticipant = tie.getOwner().getId().equals(currentUserId)
                || tie.getMember().getId().equals(currentUserId);
        if (!isParticipant) {
            throw new ResourceNotFoundException("FamilyMember", tieId);
        }

        familyMemberRepository.delete(tie);
    }
}
