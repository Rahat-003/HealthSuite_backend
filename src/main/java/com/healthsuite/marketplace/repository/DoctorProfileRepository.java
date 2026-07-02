package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.DoctorProfile;
import com.healthsuite.marketplace.enums.DoctorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorProfileRepository extends JpaRepository<DoctorProfile, Long> {

    Optional<DoctorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByLicenseNumber(String licenseNumber);

    Page<DoctorProfile> findByStatus(DoctorStatus status, Pageable pageable);

    long countByStatus(DoctorStatus status);

    @Query("""
        SELECT d FROM DoctorProfile d
        WHERE d.status = 'ACTIVE'
        AND (:specialty = '' OR LOWER(d.specialty) = LOWER(:specialty))
        AND (:search = '' OR
             LOWER(d.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(d.specialty) LIKE LOWER(CONCAT('%', :search, '%')) OR
             LOWER(d.hospitalAffiliation) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY d.rating DESC, d.totalConsultations DESC
        """)
    Page<DoctorProfile> searchActive(
        @Param("specialty") String specialty,
        @Param("search") String search,
        Pageable pageable
    );
}
