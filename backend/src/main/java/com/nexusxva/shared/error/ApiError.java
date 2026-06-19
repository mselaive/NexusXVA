package com.nexusxva.shared.error;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldViolation> details,
        Map<String, Object> metadata
) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(Instant.now(), status, error, message, path, List.of(), Map.of());
    }

    public static ApiError withDetails(
            int status,
            String error,
            String message,
            String path,
            List<FieldViolation> details
    ) {
        return new ApiError(Instant.now(), status, error, message, path, List.copyOf(details), Map.of());
    }

    public static ApiError withMetadata(
            int status,
            String error,
            String message,
            String path,
            Map<String, Object> metadata
    ) {
        return new ApiError(
                Instant.now(),
                status,
                error,
                message,
                path,
                List.of(),
                Map.copyOf(metadata)
        );
    }
}
