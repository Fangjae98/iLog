package com.ilog.ilog.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 임시 비밀번호 발급 응답 (AUTH-03, U8).
 *
 * <p>프론트가 "a***@gmail.com 으로 보냈어요" 를 서버 값으로 표시한다.
 * 마스킹을 서버가 하는 이유는 프론트 두 브랜치가 같은 규칙을 각자 구현하지 않게 하기 위함이다.
 */
public record TemporaryPasswordResponse(
        @Schema(description = "임시 비밀번호를 보낸 이메일(가림)", example = "p***@gmail.com")
        String email
) {

    /**
     * {@code @} 앞 첫 글자 + {@code ***} + {@code @도메인} (U8).
     * 예: {@code parkkitaek@gmail.com} → {@code p***@gmail.com}
     */
    public static TemporaryPasswordResponse of(String email) {
        return new TemporaryPasswordResponse(mask(email));
    }

    private static String mask(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {          // @ 가 없거나 로컬 파트가 비었으면 통째로 가린다
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
