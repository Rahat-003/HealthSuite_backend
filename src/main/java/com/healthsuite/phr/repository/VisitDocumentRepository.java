package com.healthsuite.phr.repository;

import com.healthsuite.phr.entity.VisitDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VisitDocumentRepository extends JpaRepository<VisitDocument, Long> {

    List<VisitDocument> findByVisitId(Long visitId);
}
