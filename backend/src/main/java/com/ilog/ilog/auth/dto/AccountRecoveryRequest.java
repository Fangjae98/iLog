package com.ilog.ilog.auth.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 탈퇴 계정 복구 요청 (AUTH-04). 로그인과 같이 이메일·비밀번호로 본인을 확인한다.
 * 이메일 형식은 검사하지 않는다. 틀린 형식도 "없는 계정"과 같게 401 LOGIN_FAILED 다.
 */
public record AccountRecoveryRequest(
        @Schema(description = "가입한 이메일. 대소문자를 구분하지 않는다.", example = "user@example.com")
        @NotBlank String email,
        @Schema(description = "비밀번호", example = "Passw0rd!", format = "password")
        @NotBlank String password
) {

    public AccountRecoveryRequest {
        email = UserPolicy.trim(email);   // 비밀번호는 공백도 그대로 비교한다
    }

    /** 로그에 비밀번호가 남지 않도록 가린다. */
    @Override
    public String toString() {
        return "AccountRecoveryRequest[email=" + email + "]";
    }
}
