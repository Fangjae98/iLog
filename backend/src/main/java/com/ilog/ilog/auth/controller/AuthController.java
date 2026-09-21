package com.ilog.ilog.auth.controller;

import com.ilog.ilog.auth.dto.LoginRequest;
import com.ilog.ilog.auth.dto.LoginResponse;
import com.ilog.ilog.auth.service.AuthService;
import com.ilog.ilog.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "로그인 (토큰 발급)",
            description = """
                    이메일·비밀번호로 accessToken 을 발급한다. 로그인 없이 호출한다.
                    이후 로그인이 필요한 API 는 `Authorization: Bearer {accessToken}` 헤더로 호출한다.

                    리프레시 토큰은 없다. `expiresIn`(초)이 지나면 401 `UNAUTHORIZED` 가 나오고 다시 로그인한다.
                    `passwordResetRequired` 가 true 면 임시 비밀번호로 로그인한 것이다.""")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — 이메일·비밀번호 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`LOGIN_FAILED` — 없는 이메일, 틀린 비밀번호, 탈퇴 회원 (구분하지 않는다)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/tokens")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }
}
