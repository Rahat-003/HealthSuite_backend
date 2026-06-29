package com.healthsuite.common.exception;

import org.springframework.http.HttpStatus;

public class AccountMergeException extends AppException {

    private final String existingProvider;
    private final String email;

    public AccountMergeException(String email, String existingProvider) {
        super("An account with email " + email + " already exists via " + existingProvider
                + " login. Account merging across providers is not supported.", HttpStatus.CONFLICT);
        this.existingProvider = existingProvider;
        this.email = email;
    }

    public String getExistingProvider() {
        return existingProvider;
    }

    public String getEmail() {
        return email;
    }
}
