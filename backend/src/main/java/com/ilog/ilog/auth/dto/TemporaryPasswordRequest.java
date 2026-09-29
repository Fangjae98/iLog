package com.ilog.ilog.auth.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 임시 비밀번호 발급 요청 (AUTH-03). 이메일과 이름이 모두 맞아야 발급한다. */
public record TemporaryPasswordRequest(
        @Schema(description = "가입한 이메일", example = "user@example.com")
        @NotBlank @Email @Size(max = 100) String email,

        @Schema(description = "가입할 때 입력한 이름", example = "박기택")
        @NotBlank @Size(max = 50) String name
) {

    public TemporaryPasswordRequest {
        email = UserPolicy.trim(email);
        name = UserPolicy.trim(name);
    }
}
