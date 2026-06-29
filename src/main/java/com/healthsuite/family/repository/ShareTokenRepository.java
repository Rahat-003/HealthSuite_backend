package com.healthsuite.family.repository;

import com.healthsuite.family.entity.ShareToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShareTokenRepository extends JpaRepository<ShareToken, Long> {

    Optional<ShareToken> findByTokenHashAndIsRevokedFalse(String tokenHash);

    List<ShareToken> findByOwnerUserIdAndIsRevokedFalse(Long ownerUserId);

    Optional<ShareToken> findByIdAndOwnerUserId(Long id, Long ownerUserId);
}
