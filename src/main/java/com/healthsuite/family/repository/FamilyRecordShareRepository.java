package com.healthsuite.family.repository;

import com.healthsuite.family.entity.FamilyRecordShare;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FamilyRecordShareRepository extends JpaRepository<FamilyRecordShare, Long> {

    Optional<FamilyRecordShare> findByGrantorUserIdAndGranteeUserId(Long grantorUserId, Long granteeUserId);

    List<FamilyRecordShare> findAllByGrantorUserId(Long grantorUserId);

    List<FamilyRecordShare> findAllByGranteeUserId(Long granteeUserId);
}
