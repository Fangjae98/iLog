package com.ilog.ilog.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PasswordVerificationRequest(
        @Schema(description = "현재 비밀번호", example = "Passw0rd!", format = "password")
        @NotBlank String password
) {

    /** 로그에 비밀번호가 남지 않도록 가린다. */
    @Override
    public String toString() {
        return "PasswordVerificationRequest[]";
    }
}
