package com.ilog.ilog.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 회원 탈퇴 요청 (MBR-08). 본인 확인용 현재 비밀번호만 받는다. */
public record WithdrawalRequest(
        @Schema(description = "현재 비밀번호", example = "Passw0rd!", format = "password")
        @NotBlank String password
) {

    /** 로그에 비밀번호가 남지 않도록 가린다. */
    @Override
    public String toString() {
        return "WithdrawalRequest[]";
    }
}
