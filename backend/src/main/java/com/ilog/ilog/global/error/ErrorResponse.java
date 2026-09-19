package com.ilog.ilog.global.error;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        int status,
        String code,
        String message,
        List<FieldError> errors,
        LocalDateTime timestamp
) {
    public record FieldError(String field, String reason) {}

    public static ErrorResponse of(ErrorCode ec) {
        return of(ec, List.of());
    }

    public static ErrorResponse of(ErrorCode ec, List<FieldError> errors) {
        return new ErrorResponse(ec.getStatus().value(), ec.name(),
                ec.getMessage(), errors, LocalDateTime.now());
    }
}
