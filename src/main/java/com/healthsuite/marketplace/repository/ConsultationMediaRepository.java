package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.ConsultationMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultationMediaRepository extends JpaRepository<ConsultationMedia, Long> {
    List<ConsultationMedia> findByConsultationId(Long consultationId);
}
