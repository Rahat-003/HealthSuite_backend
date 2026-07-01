package com.healthsuite.phr.repository;

import com.healthsuite.phr.entity.VisitDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisitDocumentRepository extends JpaRepository<VisitDocument, Long> {

    List<VisitDocument> findByVisitId(Long visitId);

    Optional<VisitDocument> findByIdAndVisitId(Long id, Long visitId);
}
