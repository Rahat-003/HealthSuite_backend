package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.DoctorDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DoctorDocumentRepository extends JpaRepository<DoctorDocument, Long> {
    List<DoctorDocument> findByDoctorProfileId(Long doctorProfileId);
}
