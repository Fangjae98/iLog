package com.ilog.ilog.user.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @Schema(description = "로그인 ID. 수정할 수 없다. 대소문자를 구분하지 않고 소문자로 저장한다.", example = "user@example.com")
        @NotBlank @Email @Size(max = 100) String email,
        @Schema(description = "8~20자, 영문·숫자·특수문자(!@#$%^&*) 각 1개 이상. 위반하면 400 INVALID_PASSWORD_FORMAT. 비밀번호 확인값은 프론트에서만 비교하고 서버로 보내지 않는다.",
                example = "Passw0rd!", format = "password")
        @NotBlank String password,          // 정규식은 서비스에서 검사 → INVALID_PASSWORD_FORMAT
        @Schema(description = "실명. 수정할 수 없고 마이페이지 외에는 노출하지 않는다.", example = "박기택")
        @NotBlank @Size(max = 50) String name,
        @Schema(description = "2~10자의 한글·영문·숫자. 중복 불가.", example = "기택")
        @NotBlank @Pattern(regexp = UserPolicy.NICKNAME_REGEX, message = UserPolicy.NICKNAME_MESSAGE) String nickname
) {

    public SignupRequest {
        email = UserPolicy.trim(email);
        name = UserPolicy.trim(name);
        nickname = UserPolicy.trim(nickname);
    }

    /** 로그에 비밀번호가 남지 않도록 가린다. */
    @Override
    public String toString() {
        return "SignupRequest[email=" + email + ", name=" + name + ", nickname=" + nickname + "]";
    }
}
