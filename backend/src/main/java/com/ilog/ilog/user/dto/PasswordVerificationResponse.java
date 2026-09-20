package com.ilog.ilog.user.dto;

import com.ilog.ilog.user.domain.User;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/** 마이페이지 진입 시 비밀번호 재확인 후 내려주는 개인정보. */
public record PasswordVerificationResponse(
        @Schema(description = "이메일", example = "user@example.com") String email,
        @Schema(description = "실명 (마이페이지 전용)", example = "박기택") String name,
        @Schema(description = "닉네임", example = "기택") String nickname,
        @Schema(description = "가입 일시 (KST)", example = "2026-09-16T10:00:00") LocalDateTime createdAt,
        @Schema(description = "회원정보 수정 일시. 수정 이력이 없으면 null", nullable = true, example = "2026-09-18T09:30:00")
        LocalDateTime updatedAt
) {

    public static PasswordVerificationResponse from(User user) {
        return new PasswordVerificationResponse(user.getEmail(), user.getName(), user.getNickname(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
