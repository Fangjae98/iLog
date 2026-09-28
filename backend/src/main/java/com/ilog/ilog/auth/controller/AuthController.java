package com.ilog.ilog.auth.controller;

import com.ilog.ilog.auth.dto.LoginRequest;
import com.ilog.ilog.auth.dto.LoginResponse;
import com.ilog.ilog.auth.dto.TemporaryPasswordRequest;
import com.ilog.ilog.auth.dto.TemporaryPasswordResponse;
import com.ilog.ilog.auth.service.AuthService;
import com.ilog.ilog.auth.service.TemporaryPasswordService;
import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.global.error.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final TemporaryPasswordService temporaryPasswordService;

    @Operation(summary = "로그인 (토큰 발급)",
            description = """
                    이메일·비밀번호로 accessToken 을 발급한다. 로그인 없이 호출한다.
                    이후 로그인이 필요한 API 는 `Authorization: Bearer {accessToken}` 헤더로 호출한다.

                    리프레시 토큰은 없다. `expiresIn`(초)이 지나면 401 `UNAUTHORIZED` 가 나오고 다시 로그인한다.
                    `passwordResetRequired` 가 true 면 임시 비밀번호로 로그인한 것이다.""")
    @ApiResponse(responseCode = "200", description = "로그인 성공")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — 이메일·비밀번호 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`LOGIN_FAILED` — 없는 이메일, 틀린 비밀번호, 탈퇴 후 30일이 지난 계정 (구분하지 않는다)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "`USER_WITHDRAWN` — 비밀번호는 맞지만 탈퇴 후 30일 이내. 프론트는 복구 안내를 띄운다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/tokens")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request.email(), request.password());
    }

    @Operation(summary = "로그아웃 (AUTH-02)",
            description = """
                    서버는 아무것도 저장하지 않고 204 만 돌려준다 (A4).
                    토큰 저장소(블랙리스트)가 없는 구조라, 실제 로그아웃은 프론트가 토큰을 지우는 것이다.
                    발급된 토큰은 만료까지 유효하다.""")
    @ApiResponse(responseCode = "204", description = "로그아웃 처리됨")
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 토큰 없음·위조·만료, 탈퇴 회원",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/tokens")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Login LoginUser loginUser) {
        // loginUser 를 쓰지 않지만 지우면 안 된다.
        //  - T08 로 permitAll 을 풀기 전에는 이 파라미터가 "토큰 없음 → 401" 을 만드는 유일한 장치다
        //    (LoginUserArgumentResolver 가 인증 정보를 못 찾으면 UNAUTHORIZED 를 던진다).
        //  - OpenApiConfig 가 @Login 파라미터 유무로 Swagger 에 자물쇠를 붙인다.
    }

    @Operation(summary = "임시 비밀번호 발급 (AUTH-03)",
            description = """
                    이메일과 이름이 모두 맞는 회원에게 임시 비밀번호를 메일로 보낸다. 로그인 없이 호출한다.

                    메일 발송에 성공한 뒤에만 비밀번호가 바뀐다. 발송에 실패하면 기존 비밀번호가 그대로 남는다.
                    임시 비밀번호는 24시간 동안만 쓸 수 있고(U6), 만료 후 로그인하면 401 `TEMP_PASSWORD_EXPIRED` 다.
                    응답의 이메일은 가려서 내려준다(예: `p***@gmail.com`).""")
    @ApiResponse(responseCode = "200", description = "발송 성공. 가려진 수신 이메일을 돌려준다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — 이메일 형식·이름 누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "`USER_NOT_FOUND` — 이메일·이름이 맞는 회원이 없음 (탈퇴 회원 포함)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "`MAIL_SEND_FAILED` — 메일 발송 실패. 기존 비밀번호는 그대로다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/temporary-passwords")
    public TemporaryPasswordResponse issueTemporaryPassword(
            @Valid @RequestBody TemporaryPasswordRequest request) {
        return temporaryPasswordService.issue(request.email(), request.name());
    }
}
