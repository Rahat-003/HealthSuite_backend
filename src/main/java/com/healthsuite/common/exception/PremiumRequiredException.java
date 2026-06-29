package com.healthsuite.common.exception;

import org.springframework.http.HttpStatus;

public class PremiumRequiredException extends AppException {

    public PremiumRequiredException() {
        super("Premium subscription required to perform this action", HttpStatus.FORBIDDEN);
    }
}
