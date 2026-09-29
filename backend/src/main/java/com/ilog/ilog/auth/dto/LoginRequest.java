package com.ilog.ilog.auth.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청. 이메일 형식(@Email)은 검사하지 않는다.
 * 형식이 틀린 이메일도 "없는 계정"과 같게 401 LOGIN_FAILED 로 응답한다.
 */
public record LoginRequest(
        @Schema(description = "가입한 이메일. 대소문자를 구분하지 않는다.", example = "user@example.com")
        @NotBlank String email,
        @Schema(description = "비밀번호", example = "Passw0rd!", format = "password")
        @NotBlank String password
) {

    public LoginRequest {
        email = UserPolicy.trim(email);   // 비밀번호는 공백도 그대로 비교한다
    }

    /** 로그에 비밀번호가 남지 않도록 가린다. */
    @Override
    public String toString() {
        return "LoginRequest[email=" + email + "]";
    }
}
