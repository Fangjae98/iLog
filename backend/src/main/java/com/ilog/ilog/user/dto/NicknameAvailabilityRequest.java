package com.ilog.ilog.user.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NicknameAvailabilityRequest(
        @Schema(description = "확인할 닉네임. 2~10자의 한글·영문·숫자.", example = "기택")
        @NotBlank @Pattern(regexp = UserPolicy.NICKNAME_REGEX, message = UserPolicy.NICKNAME_MESSAGE) String nickname
) {

    public NicknameAvailabilityRequest {
        nickname = UserPolicy.trim(nickname);
    }
}
