package com.healthsuite.auth.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

/**
 * Authentication object set when a valid X-Share-Token is present.
 * Grants access to a specific record without requiring full user authentication.
 */
public class ShareTokenAuthentication extends AbstractAuthenticationToken {

    private final Long ownerUserId;
    private final Long recordId;
    private final String recordType;

    public ShareTokenAuthentication(Long ownerUserId, Long recordId, String recordType) {
        super(List.of(new SimpleGrantedAuthority("ROLE_SHARE_TOKEN_READER")));
        this.ownerUserId = ownerUserId;
        this.recordId = recordId;
        this.recordType = recordType;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return ownerUserId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public Long getRecordId() {
        return recordId;
    }

    public String getRecordType() {
        return recordType;
    }
}
