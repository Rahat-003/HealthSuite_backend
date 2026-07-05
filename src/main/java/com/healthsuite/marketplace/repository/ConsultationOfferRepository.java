package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.ConsultationOffer;
import com.healthsuite.marketplace.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ConsultationOfferRepository extends JpaRepository<ConsultationOffer, Long> {

    Optional<ConsultationOffer> findByRequestIdAndDoctorProfileId(Long requestId, Long doctorProfileId);

    boolean existsByRequestIdAndDoctorProfileId(Long requestId, Long doctorProfileId);

    @Query("""
        SELECT o FROM ConsultationOffer o
        JOIN FETCH o.request r
        WHERE o.doctorProfile.id = :doctorProfileId
        AND o.status = 'PENDING'
        AND r.status = 'QUEUED'
        AND (r.expiresAt IS NULL OR r.expiresAt > :now)
        ORDER BY r.createdAt ASC
        """)
    List<ConsultationOffer> findPendingQueueForDoctor(
        @Param("doctorProfileId") Long doctorProfileId,
        @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT o FROM ConsultationOffer o
        JOIN FETCH o.request r
        WHERE o.doctorProfile.id = :doctorProfileId
        AND o.status = :status
        ORDER BY o.respondedAt DESC
        """)
    List<ConsultationOffer> findByDoctorProfileIdAndStatus(
        @Param("doctorProfileId") Long doctorProfileId,
        @Param("status") OfferStatus status
    );

    @Query("""
        SELECT o FROM ConsultationOffer o
        JOIN FETCH o.request r
        JOIN FETCH o.doctorProfile d
        WHERE d.userId = :doctorUserId
        AND o.status = 'ACCEPTED'
        AND r.status = 'ACTIVE'
        """)
    List<ConsultationOffer> findActiveAcceptedForDoctorUser(@Param("doctorUserId") Long doctorUserId);
}
