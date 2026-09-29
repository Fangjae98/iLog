package com.ilog.ilog.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record NicknameAvailabilityResponse(
        @Schema(description = "사용 가능 여부. 탈퇴 후 30일 이내 회원의 닉네임과 본인의 현재 닉네임은 false", example = "true") boolean available
) {
}
