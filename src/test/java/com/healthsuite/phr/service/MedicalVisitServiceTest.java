package com.healthsuite.phr.service;

import com.healthsuite.auth.security.ShareTokenAuthentication;
import com.healthsuite.common.exception.FamilyAccessDeniedException;
import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.exception.ShareTokenException;
import com.healthsuite.common.response.PagedResponse;
import com.healthsuite.family.entity.FamilyRecordShare;
import com.healthsuite.family.enums.ShareScope;
import com.healthsuite.family.service.FamilyMeshService;
import com.healthsuite.family.service.FamilyRecordShareService;
import com.healthsuite.phr.dto.request.CreateVisitRequest;
import com.healthsuite.phr.dto.response.MedicalVisitResponse;
import com.healthsuite.phr.entity.MedicalVisit;
import com.healthsuite.phr.entity.VisitDocument;
import com.healthsuite.phr.repository.MedicalVisitRepository;
import com.healthsuite.phr.repository.VisitDocumentRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicalVisitServiceTest {

    private static final Long OWNER_ID = 1L;
    private static final Long RELATIVE_ID = 2L;
    private static final Long STRANGER_ID = 3L;
    private static final Long VISIT_ID = 100L;

    @Mock private MedicalVisitRepository visitRepository;
    @Mock private VisitDocumentRepository documentRepository;
    @Mock private FileStorageService storageService;
    @Mock private FamilyMeshService familyMeshService;
    @Mock private FamilyRecordShareService recordShareService;

    @InjectMocks private MedicalVisitService service;

    private static MedicalVisit visit(Long id, Long ownerId) {
        return MedicalVisit.builder()
                .id(id)
                .userId(ownerId)
                .visitDate(LocalDate.of(2026, 9, 1))
                .doctorName("Dr. Rahman")
                .build();
    }

    @Nested
    class GetVisit {

        @Test
        void ownerCanReadOwnVisit() {
            when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, OWNER_ID)));

            MedicalVisitResponse response = service.getVisit(VISIT_ID, OWNER_ID, null);

            assertThat(response.id()).isEqualTo(VISIT_ID);
            verify(familyMeshService, never()).canAccess(anyLong(), anyLong());
        }

        @Test
        void verifiedRelativeWithGrantCanRead() {
            when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, OWNER_ID)));
            when(familyMeshService.canAccess(RELATIVE_ID, OWNER_ID)).thenReturn(true);
            when(recordShareService.allowsVisit(OWNER_ID, RELATIVE_ID, VISIT_ID)).thenReturn(true);

            assertThat(service.getVisit(VISIT_ID, RELATIVE_ID, null).userId()).isEqualTo(OWNER_ID);
        }

        @Test
        void verifiedRelativeWithoutGrantIsDenied() {
            when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, OWNER_ID)));
            when(familyMeshService.canAccess(RELATIVE_ID, OWNER_ID)).thenReturn(true);
            when(recordShareService.allowsVisit(OWNER_ID, RELATIVE_ID, VISIT_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.getVisit(VISIT_ID, RELATIVE_ID, null))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void strangerIsDeniedWithoutCheckingGrants() {
            when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, OWNER_ID)));
            when(familyMeshService.canAccess(STRANGER_ID, OWNER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.getVisit(VISIT_ID, STRANGER_ID, null))
                    .isInstanceOf(AccessDeniedException.class);
            verify(recordShareService, never()).allowsVisit(anyLong(), anyLong(), anyLong());
        }

        @Test
        void shareTokenForThisVisitGrantsAccess() {
            when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, OWNER_ID)));
            var token = new ShareTokenAuthentication(OWNER_ID, VISIT_ID, "VISIT");

            assertThat(service.getVisit(VISIT_ID, null, token).id()).isEqualTo(VISIT_ID);
        }

        @Test
        void shareTokenForAnotherVisitIsRejected() {
            when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.of(visit(VISIT_ID, OWNER_ID)));
            var tokenForOtherVisit = new ShareTokenAuthentication(OWNER_ID, 999L, "VISIT");

            assertThatThrownBy(() -> service.getVisit(VISIT_ID, null, tokenForOtherVisit))
                    .isInstanceOf(ShareTokenException.class);
        }

        @Test
        void missingVisitIsNotFound() {
            when(visitRepository.findById(VISIT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getVisit(VISIT_ID, OWNER_ID, null))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    class GetFamilyMemberVisits {

        private final Pageable pageable = PageRequest.of(0, 20);

        @Test
        void allScopeReturnsEveryVisit() {
            FamilyRecordShare grant = FamilyRecordShare.builder().scope(ShareScope.ALL).build();
            Page<MedicalVisit> page = new PageImpl<>(List.of(visit(1L, OWNER_ID), visit(2L, OWNER_ID)));
            when(familyMeshService.canAccess(RELATIVE_ID, OWNER_ID)).thenReturn(true);
            when(recordShareService.findGrant(OWNER_ID, RELATIVE_ID)).thenReturn(Optional.of(grant));
            when(visitRepository.findByUserIdOrderByVisitDateDesc(OWNER_ID, pageable)).thenReturn(page);

            PagedResponse<MedicalVisitResponse> result =
                    service.getFamilyMemberVisits(OWNER_ID, RELATIVE_ID, pageable);

            assertThat(result.content()).hasSize(2);
        }

        @Test
        void selectedScopeOnlyQueriesGrantedVisitIds() {
            FamilyRecordShare grant = FamilyRecordShare.builder()
                    .scope(ShareScope.SELECTED)
                    .visitIds(Set.of(7L))
                    .build();
            when(familyMeshService.canAccess(RELATIVE_ID, OWNER_ID)).thenReturn(true);
            when(recordShareService.findGrant(OWNER_ID, RELATIVE_ID)).thenReturn(Optional.of(grant));
            when(visitRepository.findByUserIdAndIdInOrderByVisitDateDesc(OWNER_ID, Set.of(7L), pageable))
                    .thenReturn(new PageImpl<>(List.of(visit(7L, OWNER_ID))));

            var result = service.getFamilyMemberVisits(OWNER_ID, RELATIVE_ID, pageable);

            assertThat(result.content()).extracting(MedicalVisitResponse::id).containsExactly(7L);
            verify(visitRepository, never()).findByUserIdOrderByVisitDateDesc(anyLong(), any());
        }

        @Test
        void relativeWithoutGrantIsDenied() {
            when(familyMeshService.canAccess(RELATIVE_ID, OWNER_ID)).thenReturn(true);
            when(recordShareService.findGrant(OWNER_ID, RELATIVE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getFamilyMemberVisits(OWNER_ID, RELATIVE_ID, pageable))
                    .isInstanceOf(FamilyAccessDeniedException.class)
                    .hasMessageContaining("has not shared");
        }

        @Test
        void nonFamilyIsDenied() {
            when(familyMeshService.canAccess(STRANGER_ID, OWNER_ID)).thenReturn(false);

            assertThatThrownBy(() -> service.getFamilyMemberVisits(OWNER_ID, STRANGER_ID, pageable))
                    .isInstanceOf(FamilyAccessDeniedException.class);
            verify(recordShareService, never()).findGrant(anyLong(), anyLong());
        }
    }

    @Nested
    class Writes {

        @Test
        void createVisitDefaultsDiagnosisTypeToDisease() {
            var request = new CreateVisitRequest(LocalDate.of(2026, 9, 1), "Dr. Rahman", null, null, null,
                    List.of(new CreateVisitRequest.DiagnosisRequest("Fever", null, null)));
            when(visitRepository.save(any(MedicalVisit.class))).thenAnswer(inv -> inv.getArgument(0));

            service.createVisit(request, OWNER_ID);

            ArgumentCaptor<MedicalVisit> saved = ArgumentCaptor.forClass(MedicalVisit.class);
            verify(visitRepository).save(saved.capture());
            assertThat(saved.getValue().getUserId()).isEqualTo(OWNER_ID);
            assertThat(saved.getValue().getDiagnoses()).singleElement()
                    .satisfies(d -> assertThat(d.getType()).isEqualTo("DISEASE"));
        }

        @Test
        void deleteVisitAlsoDeletesStoredFiles() {
            MedicalVisit v = visit(VISIT_ID, OWNER_ID);
            v.getDocuments().add(VisitDocument.builder().fileUrl("/files/a.pdf").build());
            v.getDocuments().add(VisitDocument.builder().fileUrl("/files/b.pdf").build());
            when(visitRepository.findByIdAndUserId(VISIT_ID, OWNER_ID)).thenReturn(Optional.of(v));

            service.deleteVisit(VISIT_ID, OWNER_ID);

            verify(storageService).deleteFile("/files/a.pdf");
            verify(storageService).deleteFile("/files/b.pdf");
            verify(visitRepository).delete(v);
        }

        @Test
        void cannotDeleteSomeoneElsesVisit() {
            when(visitRepository.findByIdAndUserId(eq(VISIT_ID), eq(STRANGER_ID))).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteVisit(VISIT_ID, STRANGER_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(visitRepository, never()).delete(any());
        }
    }
}
