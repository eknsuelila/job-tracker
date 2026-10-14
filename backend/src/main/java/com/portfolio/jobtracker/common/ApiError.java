package com.portfolio.jobtracker.common;

import java.time.Instant;
import java.util.Map;

/** Consistent JSON error body returned by every failed request. */
public record ApiError(Instant timestamp, int status, String error, String message, Map<String, String> fieldErrors) {

    public static ApiError of(int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, Map.of());
    }
}
