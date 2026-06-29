package com.healthsuite.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String path,
        int status,
        String error,
        String message,
        LocalDateTime timestamp,
        Map<String, String> fieldErrors
) {
    public static ErrorResponse of(String path, int status, String error, String message) {
        return new ErrorResponse(path, status, error, message, LocalDateTime.now(), null);
    }

    public static ErrorResponse withFields(String path, int status, String error,
                                           String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(path, status, error, message, LocalDateTime.now(), fieldErrors);
    }
}
