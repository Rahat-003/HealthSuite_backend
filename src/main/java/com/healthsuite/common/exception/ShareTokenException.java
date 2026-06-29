package com.healthsuite.common.exception;

import org.springframework.http.HttpStatus;

public class ShareTokenException extends AppException {

    public ShareTokenException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }
}
