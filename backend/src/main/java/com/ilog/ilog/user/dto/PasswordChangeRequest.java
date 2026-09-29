package com.ilog.ilog.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 비밀번호 변경 요청 (MBR-07).
 *
 * <p>프론트는 {@code newPasswordConfirm} 도 함께 보내지만 여기에 두지 않는다.
 * 확인값 일치는 프론트에서 검사하고, 서버는 모르는 필드를 그냥 무시한다.
 *
 * <p>비밀번호는 trim 하지 않는다 (지시서 3장). 앞뒤 공백도 비밀번호의 일부다.
 * 새 비밀번호의 형식 검사는 서비스에서 한다 ({@code INVALID_PASSWORD_FORMAT} 으로 구분하기 위해).
 */
public record PasswordChangeRequest(
        @Schema(description = "현재 비밀번호. 임시 비밀번호로 로그인한 상태면 그 임시 비밀번호다.",
                example = "Passw0rd!", format = "password")
        @NotBlank String currentPassword,

        @Schema(description = "새 비밀번호. 8~20자, 영문·숫자·특수문자(!@#$%^&*) 각 1개 이상",
                example = "NewPassw0rd!", format = "password")
        @NotBlank String newPassword
) {

    /** 로그에 비밀번호가 남지 않도록 가린다. */
    @Override
    public String toString() {
        return "PasswordChangeRequest[]";
    }
}
