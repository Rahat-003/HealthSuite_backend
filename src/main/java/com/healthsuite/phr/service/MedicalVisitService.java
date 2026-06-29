package com.healthsuite.phr.service;

import com.healthsuite.auth.security.ShareTokenAuthentication;
import com.healthsuite.common.exception.FamilyAccessDeniedException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.exception.ShareTokenException;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.family.service.FamilyMeshService;
import com.healthsuite.phr.dto.request.CreateVisitRequest;
import com.healthsuite.phr.dto.response.MedicalVisitResponse;
import com.healthsuite.phr.entity.Diagnosis;
import com.healthsuite.phr.entity.MedicalVisit;
import com.healthsuite.phr.entity.VisitDocument;
import com.healthsuite.phr.repository.MedicalVisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MedicalVisitService {

    private final MedicalVisitRepository visitRepository;
    private final FileStorageService storageService;
    private final FamilyMeshService familyMeshService;

    @Transactional
    public MedicalVisitResponse createVisit(CreateVisitRequest request, Long userId) {
        MedicalVisit visit = MedicalVisit.builder()
                .userId(userId)
                .visitDate(request.visitDate())
                .doctorName(request.doctorName())
                .doctorSpecialty(request.doctorSpecialty())
                .hospitalName(request.hospitalName())
                .notes(request.notes())
                .build();

        if (request.diagnoses() != null) {
            List<Diagnosis> diagnoses = request.diagnoses().stream()
                    .map(d -> Diagnosis.builder()
                            .visit(visit)
                            .name(d.name())
                            .type(d.type() != null ? d.type() : "DISEASE")
                            .notes(d.notes())
                            .build())
                    .toList();
            visit.getDiagnoses().addAll(diagnoses);
        }

        return MedicalVisitResponse.from(visitRepository.save(visit));
    }

    @Transactional(readOnly = true)
    public PagedResponse<MedicalVisitResponse> getMyVisits(Long userId, Pageable pageable) {
        return PagedResponse.from(
                visitRepository.findByUserIdOrderByVisitDateDesc(userId, pageable)
                        .map(MedicalVisitResponse::from));
    }

    /**
     * Core mesh authorization: owner → family member → share token.
     */
    @Transactional(readOnly = true)
    public MedicalVisitResponse getVisit(Long visitId, Long requesterId, Authentication authentication) {
        MedicalVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalVisit", visitId));

        // Share token path (no user authentication required)
        if (authentication instanceof ShareTokenAuthentication sta) {
            if (!sta.getRecordId().equals(visitId)) {
                throw new ShareTokenException("Share token is not valid for the requested record");
            }
            return MedicalVisitResponse.from(visit);
        }

        // Owner path
        if (visit.getUserId().equals(requesterId)) {
            return MedicalVisitResponse.from(visit);
        }

        // Verified family member path
        if (familyMeshService.canAccess(requesterId, visit.getUserId())) {
            return MedicalVisitResponse.from(visit);
        }

        throw new AccessDeniedException("You do not have access to this medical record");
    }

    /**
     * List visits for another user — requires verified family relationship.
     */
    @Transactional(readOnly = true)
    public PagedResponse<MedicalVisitResponse> getFamilyMemberVisits(Long targetUserId,
                                                                       Long requesterId,
                                                                       Pageable pageable) {
        if (!familyMeshService.canAccess(requesterId, targetUserId)) {
            throw new FamilyAccessDeniedException();
        }
        return PagedResponse.from(
                visitRepository.findByUserIdOrderByVisitDateDesc(targetUserId, pageable)
                        .map(MedicalVisitResponse::from));
    }

    @Transactional
    public MedicalVisitResponse attachDocument(Long visitId, Long userId, MultipartFile file) {
        MedicalVisit visit = visitRepository.findByIdAndUserId(visitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalVisit", visitId));

        String fileUrl = storageService.uploadFile(file, "visits/" + visitId);

        VisitDocument doc = VisitDocument.builder()
                .visit(visit)
                .fileUrl(fileUrl)
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .build();

        visit.getDocuments().add(doc);
        return MedicalVisitResponse.from(visitRepository.save(visit));
    }

    @Transactional
    public void deleteVisit(Long visitId, Long userId) {
        MedicalVisit visit = visitRepository.findByIdAndUserId(visitId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalVisit", visitId));
        visit.getDocuments().forEach(doc -> storageService.deleteFile(doc.getFileUrl()));
        visitRepository.delete(visit);
    }
}
