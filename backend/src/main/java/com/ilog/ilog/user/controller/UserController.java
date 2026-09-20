package com.ilog.ilog.user.controller;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.global.error.ErrorResponse;
import com.ilog.ilog.user.dto.EmailAvailabilityRequest;
import com.ilog.ilog.user.dto.EmailAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameAvailabilityRequest;
import com.ilog.ilog.user.dto.NicknameAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameUpdateRequest;
import com.ilog.ilog.user.dto.NicknameUpdateResponse;
import com.ilog.ilog.user.dto.PasswordVerificationRequest;
import com.ilog.ilog.user.dto.PasswordVerificationResponse;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.dto.SignupResponse;
import com.ilog.ilog.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "회원")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "회원가입 (MBR-01)",
            description = """
                    이메일·비밀번호·이름·닉네임으로 회원을 만든다. 로그인 없이 호출한다.
                    성공하면 프론트가 로그인 페이지로 이동한다. 이메일 인증은 MVP 에서 제외했다(D-14).

                    처리 순서: 형식 검증 → 비밀번호 정규식 → 이메일 중복(탈퇴 30일 이내 계정이면 `REJOIN_RESTRICTED`) → 닉네임 중복 → 저장.""")
    @ApiResponse(responseCode = "201", description = "가입 성공")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT`(필수값 누락·이메일 형식·이름/닉네임 규칙) 또는 `INVALID_PASSWORD_FORMAT`(비밀번호 정규식 위반)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "`USER_DUPLICATE_EMAIL` / `USER_DUPLICATE_NICKNAME` / `REJOIN_RESTRICTED`(탈퇴 후 30일 이내 이메일)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SignupResponse signup(@Valid @RequestBody SignupRequest request) {
        return new SignupResponse(userService.signup(request));
    }

    @Operation(summary = "이메일 사용 가능 확인 (MBR-02)",
            description = "로그인 없이 호출한다. 사용 중이면 `DUPLICATE`, 탈퇴 후 30일 이내 계정이면 `WITHDRAWN`(로그인하면 복구 가능)을 `reason` 으로 알려준다. 사용 가능하면 `reason` 은 null.")
    @ApiResponse(responseCode = "200", description = "확인 완료 (사용 불가여도 200)")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — 이메일 형식 오류·누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/email-availability")
    public EmailAvailabilityResponse checkEmail(@ParameterObject @Valid @ModelAttribute EmailAvailabilityRequest request) {
        return userService.checkEmail(request.email());
    }

    @Operation(summary = "닉네임 사용 가능 확인 (MBR-03)",
            description = "로그인 없이 호출한다. 회원가입과 닉네임 수정에서 함께 쓴다. 탈퇴 후 30일 이내 회원의 닉네임도 사용 중으로 본다. "
                    + "본인의 현재 닉네임을 넣으면 `false` 가 나오므로, 수정 화면에서는 프론트가 현재 닉네임과 먼저 비교한다.")
    @ApiResponse(responseCode = "200", description = "확인 완료 (사용 불가여도 200)")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — 길이·문자 규칙 위반·누락",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/nickname-availability")
    public NicknameAvailabilityResponse checkNickname(@ParameterObject @Valid @ModelAttribute NicknameAvailabilityRequest request) {
        return userService.checkNickname(request.nickname());
    }

    @Operation(summary = "비밀번호 재확인 + 개인정보 조회 (MBR-05)",
            description = "마이페이지 진입 때 1회만 호출한다(D-15). 비밀번호를 body 로 받아야 해서, 쿼리로 넘기면 서버·프록시 로그에 남기 때문에 GET 대신 POST 를 쓴다.")
    @ApiResponse(responseCode = "200", description = "비밀번호 일치. 개인정보를 돌려준다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT`(비밀번호 누락) 또는 `PASSWORD_MISMATCH`(비밀번호 불일치)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` / `TOKEN_EXPIRED` — 로그인 필요, 토큰 만료, 탈퇴 회원",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping("/me/password-verification")
    public PasswordVerificationResponse verifyPassword(@Login LoginUser loginUser,
                                                       @Valid @RequestBody PasswordVerificationRequest request) {
        return userService.verifyPassword(loginUser.userId(), request.password());
    }

    @Operation(summary = "닉네임 수정 (MBR-06)",
            description = "비밀번호 없이 호출한다(D-15). 이메일·이름은 수정할 수 없어서 body 에 있어도 무시한다.")
    @ApiResponse(responseCode = "200", description = "수정 성공. 프론트는 응답의 nickname 으로 헤더를 갱신한다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT`(길이·문자 규칙) 또는 `NICKNAME_UNCHANGED`(현재 닉네임과 같음)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` / `TOKEN_EXPIRED` — 로그인 필요, 토큰 만료, 탈퇴 회원",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "`USER_DUPLICATE_NICKNAME` — 이미 사용 중인 닉네임",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/me")
    public NicknameUpdateResponse updateNickname(@Login LoginUser loginUser,
                                                 @Valid @RequestBody NicknameUpdateRequest request) {
        return userService.updateNickname(loginUser.userId(), request.nickname());
    }
}
