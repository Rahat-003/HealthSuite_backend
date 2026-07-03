package com.healthsuite.phr.repository;

import com.healthsuite.phr.entity.MedicalVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {

    Page<MedicalVisit> findByUserIdOrderByVisitDateDesc(Long userId, Pageable pageable);

    Optional<MedicalVisit> findByIdAndUserId(Long id, Long userId);

    Optional<MedicalVisit> findFirstByConsultationId(Long consultationId);

    /** SELECTED-scope family view: only the visits the owner chose to share. */
    Page<MedicalVisit> findByUserIdAndIdInOrderByVisitDateDesc(Long userId, Collection<Long> ids, Pageable pageable);

    /** Ownership filter for share grants — silently drops ids that are not the user's. */
    @Query("SELECT v.id FROM MedicalVisit v WHERE v.userId = :userId AND v.id IN :ids")
    List<Long> findIdsByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}
