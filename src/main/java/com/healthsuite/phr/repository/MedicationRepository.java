package com.healthsuite.phr.repository;

import com.healthsuite.phr.entity.Medication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicationRepository extends JpaRepository<Medication, Long> {

    List<Medication> findByVisitId(Long visitId);

    Optional<Medication> findByIdAndVisitId(Long id, Long visitId);
}
