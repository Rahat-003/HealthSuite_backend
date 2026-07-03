package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {

    Optional<Prescription> findByConsultationId(Long consultationId);

    boolean existsByConsultationId(Long consultationId);
}
