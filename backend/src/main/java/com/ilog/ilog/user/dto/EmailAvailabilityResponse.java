package com.ilog.ilog.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record EmailAvailabilityResponse(
        @Schema(description = "사용 가능 여부", example = "false") boolean available,
        @Schema(description = "available=false 일 때만 값이 있다. DUPLICATE: 사용 중 / WITHDRAWN: 탈퇴 후 30일 이내(로그인하면 복구 가능)",
                nullable = true, example = "WITHDRAWN") Reason reason
) {

    /** DUPLICATE: 사용 중 / WITHDRAWN: 탈퇴 후 30일 이내 (로그인하면 복구 가능) */
    public enum Reason {
        DUPLICATE, WITHDRAWN
    }

    public static EmailAvailabilityResponse ofAvailable() {
        return new EmailAvailabilityResponse(true, null);
    }

    public static EmailAvailabilityResponse ofUnavailable(Reason reason) {
        return new EmailAvailabilityResponse(false, reason);
    }
}
