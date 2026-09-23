package com.fawry.lms.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(HttpStatus status, String message) {
        return new ApiErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, null);
    }

    public static ApiErrorResponse validation(Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), HttpStatus.BAD_REQUEST.value(), "BadRequest", "Validation errors", fieldErrors);
    }
}
