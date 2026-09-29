package com.ilog.ilog.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SignupResponse(
        @Schema(description = "생성된 회원 ID", example = "1") Long userId
) {
}
