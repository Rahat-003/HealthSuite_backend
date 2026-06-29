package com.healthsuite.family.service;

import com.healthsuite.common.exception.ResourceNotFoundException;
import com.healthsuite.common.exception.ShareTokenException;
import com.healthsuite.family.dto.request.CreateShareTokenRequest;
import com.healthsuite.family.dto.response.ShareTokenResponse;
import com.healthsuite.family.entity.ShareToken;
import com.healthsuite.family.repository.ShareTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShareTokenService {

    private final ShareTokenRepository shareTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public ShareTokenResponse createToken(CreateShareTokenRequest request, Long ownerUserId) {
        // Generate a cryptographically secure raw token (two UUIDs = 64 hex chars, no dashes)
        String rawToken = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");

        String tokenHash = sha256Hex(rawToken);
        Instant expiresAt = Instant.now().plus(request.ttlHours(), ChronoUnit.HOURS);

        ShareToken token = ShareToken.builder()
                .tokenHash(tokenHash)
                .ownerUserId(ownerUserId)
                .recordType(request.recordType())
                .recordId(request.recordId())
                .expiresAt(expiresAt)
                .build();

        shareTokenRepository.save(token);

        // Return raw token ONCE — it is never stored and cannot be recovered
        return ShareTokenResponse.created(token.getId(), rawToken, request.recordType(),
                request.recordId(), expiresAt);
    }

    @Transactional
    public void revokeToken(Long tokenId, Long requestingUserId) {
        ShareToken token = shareTokenRepository.findByIdAndOwnerUserId(tokenId, requestingUserId)
                .orElseThrow(() -> new ResourceNotFoundException("ShareToken", tokenId));

        token.setRevoked(true);
        shareTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public List<ShareTokenResponse> getMyActiveTokens(Long userId) {
        return shareTokenRepository.findByOwnerUserIdAndIsRevokedFalse(userId).stream()
                .filter(t -> !t.isExpired())
                .map(ShareTokenResponse::fromExisting)
                .toList();
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
