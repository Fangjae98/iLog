package com.ilog.ilog.user.controller;

import com.ilog.ilog.global.config.SecurityConfig;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.dto.EmailAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameUpdateResponse;
import com.ilog.ilog.user.dto.PasswordVerificationResponse;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    // ---------- MBR-01 회원가입 ----------

    @Test
    void 회원가입하면_201과_userId() throws Exception {
        when(userService.signup(any(SignupRequest.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Passw0rd!","name":"박기택","nickname":"기택"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void 문자열_앞뒤_공백은_제거한_뒤_서비스로_넘긴다() throws Exception {
        when(userService.signup(any(SignupRequest.class))).thenReturn(1L);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"  user@example.com ","password":"Passw0rd!","name":" 박기택 ","nickname":" 기택 "}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<SignupRequest> captor = ArgumentCaptor.forClass(SignupRequest.class);
        verify(userService).signup(captor.capture());
        assertThat(captor.getValue().email()).isEqualTo("user@example.com");
        assertThat(captor.getValue().name()).isEqualTo("박기택");
        assertThat(captor.getValue().nickname()).isEqualTo("기택");
    }

    @Test
    void 필수값이_비었거나_형식이_틀리면_400과_필드별_사유() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-email","password":"","name":"   ","nickname":"a"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[?(@.field=='email')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='password')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field=='nickname')].reason")
                        .value("닉네임은 2~10자의 한글, 영문, 숫자만 가능합니다."));
        verifyNoInteractions(userService);
    }

    @Test
    void 이메일이_100자를_넘으면_400() throws Exception {
        String longEmail = "a".repeat(92) + "@abc.com";   // 100자 초과

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Passw0rd!","name":"박기택","nickname":"기택"}""".formatted(longEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void 닉네임에_특수문자가_있으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"a@b.com","password":"Passw0rd!","name":"박기택","nickname":"기택!"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
    }

    @Test
    void 서비스의_비즈니스_예외는_ErrorCode의_상태와_코드로_응답한다() throws Exception {
        when(userService.signup(any(SignupRequest.class)))
                .thenThrow(new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT))
                .thenThrow(new BusinessException(ErrorCode.USER_DUPLICATE_EMAIL))
                .thenThrow(new BusinessException(ErrorCode.USER_DUPLICATE_NICKNAME))
                .thenThrow(new BusinessException(ErrorCode.REJOIN_RESTRICTED));
        String body = """
                {"email":"a@b.com","password":"Passw0rd!","name":"박기택","nickname":"기택"}""";

        expectSignupError(body, 400, "INVALID_PASSWORD_FORMAT");
        expectSignupError(body, 409, "USER_DUPLICATE_EMAIL");
        expectSignupError(body, 409, "USER_DUPLICATE_NICKNAME");
        expectSignupError(body, 409, "REJOIN_RESTRICTED");
    }

    // ---------- MBR-02 / MBR-03 중복 확인 ----------

    @Test
    void 이메일이_사용_가능하면_reason은_null() throws Exception {
        when(userService.checkEmail("a@b.com")).thenReturn(EmailAvailabilityResponse.ofAvailable());

        mockMvc.perform(get("/api/v1/users/email-availability").param("email", "a@b.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.reason").value((Object) null));
    }

    @Test
    void 탈퇴_계정_이메일이면_WITHDRAWN() throws Exception {
        when(userService.checkEmail("a@b.com"))
                .thenReturn(EmailAvailabilityResponse.ofUnavailable(EmailAvailabilityResponse.Reason.WITHDRAWN));

        mockMvc.perform(get("/api/v1/users/email-availability").param("email", "a@b.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.reason").value("WITHDRAWN"));
    }

    @Test
    void 이메일_쿼리는_앞뒤_공백을_제거한_뒤_검증한다() throws Exception {
        when(userService.checkEmail("a@b.com")).thenReturn(EmailAvailabilityResponse.ofAvailable());

        mockMvc.perform(get("/api/v1/users/email-availability").param("email", " a@b.com "))
                .andExpect(status().isOk());
        verify(userService).checkEmail("a@b.com");
    }

    @Test
    void 이메일_형식이_틀리거나_없으면_400() throws Exception {
        mockMvc.perform(get("/api/v1/users/email-availability").param("email", "not-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
        mockMvc.perform(get("/api/v1/users/email-availability"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verify(userService, never()).checkEmail(anyString());
    }

    @Test
    void 닉네임_사용_가능_여부() throws Exception {
        when(userService.checkNickname("기택")).thenReturn(new NicknameAvailabilityResponse(false));

        mockMvc.perform(get("/api/v1/users/nickname-availability").param("nickname", "기택"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void 닉네임_길이나_문자_규칙을_어기면_400() throws Exception {
        mockMvc.perform(get("/api/v1/users/nickname-availability").param("nickname", "가"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
        mockMvc.perform(get("/api/v1/users/nickname-availability").param("nickname", "열한글자넘는닉네임입니다"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/users/nickname-availability"))
                .andExpect(status().isBadRequest());
        verify(userService, never()).checkNickname(anyString());
    }

    // ---------- MBR-05 비밀번호 재확인 ----------

    @Test
    void 로그인_없이_재확인하면_401() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(userService);
    }

    @Test
    void 재확인에_성공하면_개인정보를_돌려준다() throws Exception {
        when(userService.verifyPassword(1L, "Passw0rd!")).thenReturn(new PasswordVerificationResponse(
                "user@example.com", "박기택", "기택", LocalDateTime.of(2026, 9, 16, 10, 0, 0), null));

        mockMvc.perform(post("/api/v1/users/me/password-verification")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Passw0rd!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.name").value("박기택"))
                .andExpect(jsonPath("$.nickname").value("기택"))
                .andExpect(jsonPath("$.createdAt").value("2026-09-16T10:00:00"))
                .andExpect(jsonPath("$.updatedAt").value((Object) null));
    }

    @Test
    void 비밀번호가_틀리면_400_PASSWORD_MISMATCH() throws Exception {
        when(userService.verifyPassword(eq(1L), anyString()))
                .thenThrow(new BusinessException(ErrorCode.PASSWORD_MISMATCH));

        mockMvc.perform(post("/api/v1/users/me/password-verification")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Wrong0!pw\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PASSWORD_MISMATCH"));
    }

    @Test
    void 재확인_비밀번호가_비어있으면_400() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password-verification")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
        verify(userService, never()).verifyPassword(anyLong(), anyString());
    }

    // ---------- MBR-06 닉네임 수정 ----------

    @Test
    void 로그인_없이_닉네임을_수정하면_401() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userService);
    }

    @Test
    void 닉네임을_수정하면_200과_새_닉네임() throws Exception {
        when(userService.updateNickname(1L, "새닉네임")).thenReturn(new NicknameUpdateResponse(1L, "새닉네임"));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\" 새닉네임 \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.nickname").value("새닉네임"));
    }

    @Test
    void 닉네임_수정에서_이메일과_이름은_받지_않고_무시한다() throws Exception {
        when(userService.updateNickname(1L, "새닉네임")).thenReturn(new NicknameUpdateResponse(1L, "새닉네임"));

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"새닉네임\",\"email\":\"hack@x.com\",\"name\":\"해커\"}"))
                .andExpect(status().isOk());
        verify(userService).updateNickname(1L, "새닉네임");
    }

    @Test
    void 닉네임_규칙을_어기면_400() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
        verify(userService, never()).updateNickname(anyLong(), anyString());
    }

    @Test
    void 닉네임_수정_비즈니스_예외의_상태코드() throws Exception {
        when(userService.updateNickname(eq(1L), anyString()))
                .thenThrow(new BusinessException(ErrorCode.NICKNAME_UNCHANGED))
                .thenThrow(new BusinessException(ErrorCode.USER_DUPLICATE_NICKNAME));

        for (int[] expected : new int[][]{{400}, {409}}) {
            mockMvc.perform(patch("/api/v1/users/me")
                            .header("X-User-Id", "1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"nickname\":\"새닉네임\"}"))
                    .andExpect(status().is(expected[0]));
        }
    }

    private void expectSignupError(String body, int status, String code) throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code));
    }
}
