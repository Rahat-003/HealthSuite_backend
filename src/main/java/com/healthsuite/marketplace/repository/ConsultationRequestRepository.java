package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.ConsultationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {

    Page<ConsultationRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    List<ConsultationRequest> findByPatientId(Long patientId);

    @Query("""
        SELECT COALESCE(SUM(r.upfrontAmountBdt), 0) FROM ConsultationRequest r
        WHERE r.status IN ('QUEUED', 'ACTIVE')
        """)
    BigDecimal sumEscrowHeld();
}
