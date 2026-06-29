package com.healthsuite.scraper.repository;

import com.healthsuite.scraper.entity.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByBmdcNumber(String bmdcNumber);

    Optional<Doctor> findByFullNameAndSpecialtyAndChamberAddress(String fullName, String specialty,
                                                                  String chamberAddress);

    Page<Doctor> findByIsActiveTrueAndFullNameContainingIgnoreCase(String name, Pageable pageable);

    Page<Doctor> findByIsActiveTrueAndSpecialtyContainingIgnoreCase(String specialty, Pageable pageable);

    Page<Doctor> findByIsActiveTrue(Pageable pageable);
}
