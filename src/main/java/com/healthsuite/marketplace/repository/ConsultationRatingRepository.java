package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.ConsultationRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface ConsultationRatingRepository extends JpaRepository<ConsultationRating, Long> {

    Optional<ConsultationRating> findByConsultationId(Long consultationId);

    boolean existsByConsultationId(Long consultationId);

    @Query("SELECT COALESCE(AVG(r.stars), 0) FROM ConsultationRating r WHERE r.doctorProfile.id = :doctorProfileId")
    BigDecimal averageForDoctor(@Param("doctorProfileId") Long doctorProfileId);

    long countByDoctorProfileId(Long doctorProfileId);
}
