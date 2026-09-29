package com.ilog.ilog.auth.dto;

import com.ilog.ilog.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

/** 로그인 성공 응답. 프론트는 accessToken 을 Authorization: Bearer 헤더로 보낸다. */
public record LoginResponse(
        @Schema(description = "accessToken (JWT)", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.abc")
        String accessToken,
        @Schema(description = "accessToken 유효 시간 (초)", example = "7200")
        long expiresIn,
        UserSummary user,
        @Schema(description = "임시 비밀번호로 로그인했는지. true 면 프론트가 비밀번호 변경 화면으로 보낸다", example = "false")
        boolean passwordResetRequired
) {

    /** 헤더 표시에 필요한 최소 정보. (LoginUser 와 이름이 겹치면 Swagger 스키마가 섞여서 따로 둔다) */
    public record UserSummary(
            @Schema(description = "회원 ID", example = "1") Long userId,
            @Schema(description = "닉네임", example = "기택") String nickname
    ) {
    }

    public static LoginResponse of(String accessToken, long expiresIn, User user) {
        return new LoginResponse(accessToken, expiresIn,
                new UserSummary(user.getId(), user.getNickname()), user.isTempPassword());
    }
}
