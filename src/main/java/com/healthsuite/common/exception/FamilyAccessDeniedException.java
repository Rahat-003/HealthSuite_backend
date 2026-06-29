package com.healthsuite.common.exception;

import org.springframework.http.HttpStatus;

public class FamilyAccessDeniedException extends AppException {

    public FamilyAccessDeniedException() {
        super("Access denied: no verified family relationship exists", HttpStatus.FORBIDDEN);
    }

    public FamilyAccessDeniedException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
