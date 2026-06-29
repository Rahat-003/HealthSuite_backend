package com.healthsuite.phr.repository;

import com.healthsuite.phr.entity.MedicalVisit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MedicalVisitRepository extends JpaRepository<MedicalVisit, Long> {

    Page<MedicalVisit> findByUserIdOrderByVisitDateDesc(Long userId, Pageable pageable);

    Optional<MedicalVisit> findByIdAndUserId(Long id, Long userId);
}
