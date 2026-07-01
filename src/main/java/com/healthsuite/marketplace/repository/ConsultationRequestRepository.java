package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.ConsultationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConsultationRequestRepository extends JpaRepository<ConsultationRequest, Long> {

    Page<ConsultationRequest> findByPatientIdOrderByCreatedAtDesc(Long patientId, Pageable pageable);

    List<ConsultationRequest> findByPatientId(Long patientId);
}
