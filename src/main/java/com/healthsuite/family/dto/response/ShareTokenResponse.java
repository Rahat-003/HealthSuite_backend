package com.healthsuite.family.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShareTokenResponse(
        Long id,
        String token,  // Only populated on creation; null on list responses
        String recordType,
        Long recordId,
        Instant expiresAt
) {
    // Called once on token creation — raw token shown here and never again
    public static ShareTokenResponse created(Long id, String rawToken, String recordType,
                                              Long recordId, Instant expiresAt) {
        return new ShareTokenResponse(id, rawToken, recordType, recordId, expiresAt);
    }

    // Called when listing existing tokens — token field omitted (it was hashed, not stored)
    public static ShareTokenResponse fromExisting(com.healthsuite.family.entity.ShareToken token) {
        return new ShareTokenResponse(token.getId(), null, token.getRecordType(),
                token.getRecordId(), token.getExpiresAt());
    }
}
