package com.healthsuite.marketplace.repository;

import com.healthsuite.marketplace.entity.Specialty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {
    List<Specialty> findByIsActiveTrueOrderBySortOrderAsc();
}
