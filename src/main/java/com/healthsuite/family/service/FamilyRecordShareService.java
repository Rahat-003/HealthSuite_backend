package com.healthsuite.family.service;

import com.healthsuite.auth.repository.UserRepository;
import com.healthsuite.common.exception.BadRequestException;
import com.healthsuite.common.exception.FamilyAccessDeniedException;
import com.healthsuite.family.dto.request.UpdateRecordShareRequest;
import com.healthsuite.family.dto.response.RecordShareResponse;
import com.healthsuite.family.entity.FamilyRecordShare;
import com.healthsuite.family.enums.ShareScope;
import com.healthsuite.family.repository.FamilyMemberRepository;
import com.healthsuite.family.repository.FamilyRecordShareRepository;
import com.healthsuite.notification.NotificationService;
import com.healthsuite.phr.repository.MedicalVisitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FamilyRecordShareService {

    private final FamilyRecordShareRepository shareRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final MedicalVisitRepository visitRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RecordShareResponse> getMyShares(Long grantorUserId) {
        return shareRepository.findAllByGrantorUserId(grantorUserId).stream()
                .map(RecordShareResponse::from)
                .toList();
    }

    @Transactional
    public RecordShareResponse updateShare(Long grantorUserId, Long granteeUserId,
                                           UpdateRecordShareRequest request) {
        if (!familyMemberRepository.existsVerifiedRelationship(grantorUserId, granteeUserId)) {
            throw new FamilyAccessDeniedException();
        }
        String grantorName = userRepository.findById(grantorUserId)
                .map(u -> u.getFullName()).orElse("A family member");

        Optional<FamilyRecordShare> existing =
                shareRepository.findByGrantorUserIdAndGranteeUserId(grantorUserId, granteeUserId);

        if ("NONE".equals(request.scope())) {
            existing.ifPresent(shareRepository::delete);
            return new RecordShareResponse(granteeUserId, "NONE", List.of());
        }

        ShareScope scope = ShareScope.valueOf(request.scope());
        Set<Long> visitIds = new HashSet<>();
        if (scope == ShareScope.SELECTED) {
            if (request.visitIds() == null || request.visitIds().isEmpty()) {
                throw new BadRequestException("Select at least one record to share, or choose ALL / NONE");
            }
            // only the grantor's own visits can be shared
            visitIds.addAll(visitRepository.findIdsByUserIdAndIdIn(grantorUserId, request.visitIds()));
            if (visitIds.isEmpty()) {
                throw new BadRequestException("None of the selected records belong to you");
            }
        }

        FamilyRecordShare share = existing.orElseGet(() -> FamilyRecordShare.builder()
                .grantorUserId(grantorUserId)
                .granteeUserId(granteeUserId)
                .scope(scope)
                .build());
        share.setScope(scope);
        share.getVisitIds().clear();
        share.getVisitIds().addAll(visitIds);
        FamilyRecordShare saved = shareRepository.save(share);

        notificationService.notify(granteeUserId, "RECORDS_SHARED",
                "Health records shared with you",
                scope == ShareScope.ALL
                        ? grantorName + " shared their full health record with you."
                        : grantorName + " shared " + visitIds.size() + " health record"
                          + (visitIds.size() > 1 ? "s" : "") + " with you.",
                "/family");

        return RecordShareResponse.from(saved);
    }

    /** Grant lookup used by PHR enforcement: what may grantee see of grantor's records? */
    @Transactional(readOnly = true)
    public Optional<FamilyRecordShare> findGrant(Long grantorUserId, Long granteeUserId) {
        return shareRepository.findByGrantorUserIdAndGranteeUserId(grantorUserId, granteeUserId);
    }

    @Transactional(readOnly = true)
    public boolean allowsVisit(Long grantorUserId, Long granteeUserId, Long visitId) {
        return findGrant(grantorUserId, granteeUserId)
                .map(g -> g.getScope() == ShareScope.ALL || g.getVisitIds().contains(visitId))
                .orElse(false);
    }
}
