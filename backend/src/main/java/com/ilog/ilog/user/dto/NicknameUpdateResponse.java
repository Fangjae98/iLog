package com.ilog.ilog.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameUpdateResponse(
        @Schema(description = "회원 ID", example = "1") Long userId,
        @Schema(description = "변경된 닉네임", example = "새닉네임") String nickname
) {
}
