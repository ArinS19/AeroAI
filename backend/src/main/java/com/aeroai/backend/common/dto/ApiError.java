package com.aeroai.backend.common.dto;

import java.time.Instant;

/**
 * Binding wire-format error envelope (IMPLEMENT_v2.md Section 46.1).
 * Every non-2xx JSON error response uses exactly this shape.
 */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
    public static ApiError of(int status, String errorCode, String message, String path) {
        return new ApiError(Instant.now(), status, errorCode, message, path);
    }
}
