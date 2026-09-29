package com.ilog.ilog.user.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** 이메일·이름은 수정 불가라 필드 자체를 두지 않는다. */
public record NicknameUpdateRequest(
        @Schema(description = "새 닉네임. 2~10자의 한글·영문·숫자. 현재 닉네임과 같으면 400 `NICKNAME_UNCHANGED`, 남이 쓰고 있으면 409 `USER_DUPLICATE_NICKNAME`.", example = "새닉네임")
        @NotBlank @Pattern(regexp = UserPolicy.NICKNAME_REGEX, message = UserPolicy.NICKNAME_MESSAGE) String nickname
) {

    public NicknameUpdateRequest {
        nickname = UserPolicy.trim(nickname);
    }
}
