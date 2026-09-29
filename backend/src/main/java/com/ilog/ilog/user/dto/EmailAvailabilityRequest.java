package com.ilog.ilog.user.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailAvailabilityRequest(
        @Schema(description = "확인할 이메일. 앞뒤 공백은 제거하고 대소문자는 구분하지 않는다.", example = "user@example.com")
        @NotBlank @Email @Size(max = 100) String email
) {

    public EmailAvailabilityRequest {
        email = UserPolicy.trim(email);
    }
}
