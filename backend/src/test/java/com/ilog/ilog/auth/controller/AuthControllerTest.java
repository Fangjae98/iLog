package com.ilog.ilog.auth.controller;

import com.ilog.ilog.auth.dto.LoginResponse;
import com.ilog.ilog.auth.dto.TemporaryPasswordResponse;
import com.ilog.ilog.auth.service.AuthService;
import com.ilog.ilog.auth.service.TemporaryPasswordService;
import com.ilog.ilog.support.SecuredSliceTestSupport;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends SecuredSliceTestSupport {

    private static final String URL = "/api/v1/auth/tokens";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

    @MockitoBean
    TemporaryPasswordService temporaryPasswordService;

    @Test
    void 로그인하면_200과_토큰_회원정보() throws Exception {
        when(authService.login("user@example.com", "Passw0rd!")).thenReturn(
                new LoginResponse("token-value", 7200, new LoginResponse.UserSummary(1L, "기택"), false));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Passw0rd!"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("token-value"))
                .andExpect(jsonPath("$.expiresIn").value(7200))
                .andExpect(jsonPath("$.user.userId").value(1))
                .andExpect(jsonPath("$.user.nickname").value("기택"))
                .andExpect(jsonPath("$.passwordResetRequired").value(false));
    }

    @Test
    void 이메일_앞뒤_공백만_제거하고_비밀번호는_그대로_넘긴다() throws Exception {
        mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"  user@example.com ","password":" Passw0rd! "}"""));

        verify(authService).login("user@example.com", " Passw0rd! ");
    }

    @Test
    void 이메일이나_비밀번호가_비어_있으면_400_INVALID_INPUT() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"  ","password":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors.length()").value(2));

        verifyNoInteractions(authService);
    }

    @Test
    void 로그인_실패면_401_LOGIN_FAILED() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new BusinessException(ErrorCode.LOGIN_FAILED));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Wrong123!"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @Test
    void 탈퇴_30일_이내_계정이면_403_USER_WITHDRAWN() throws Exception {
        // 프론트는 이 코드를 보고 복구 안내를 띄운다 (U2, 지시서 8장)
        when(authService.login(anyString(), anyString())).thenThrow(new BusinessException(ErrorCode.USER_WITHDRAWN));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Passw0rd!"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_WITHDRAWN"));
    }

    @Test
    void 로그아웃하면_204이고_서버는_아무것도_하지_않는다() throws Exception {
        // A4: 토큰 저장소가 없어 서버 상태 변화가 없다. 실제 로그아웃은 프론트가 토큰을 지우는 것.
        mockMvc.perform(delete(URL)
                        .header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verifyNoInteractions(authService);
    }

    @Test
    void 토큰_없이_로그아웃하면_401() throws Exception {
        mockMvc.perform(delete(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 위조된_토큰으로_로그아웃하면_401() throws Exception {
        mockMvc.perform(delete(URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    // ---------- 임시 비밀번호 발급 (AUTH-03) ----------

    @Test
    void 임시_비밀번호를_발급하면_가려진_이메일을_돌려준다() throws Exception {
        when(temporaryPasswordService.issue("user@example.com", "박기택"))
                .thenReturn(TemporaryPasswordResponse.of("user@example.com"));

        mockMvc.perform(post("/api/v1/auth/temporary-passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","name":"박기택"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("u***@example.com"));
    }

    @Test
    void 임시_비밀번호_발급은_로그인_없이_부른다() throws Exception {
        when(temporaryPasswordService.issue(anyString(), anyString()))
                .thenReturn(TemporaryPasswordResponse.of("user@example.com"));

        mockMvc.perform(post("/api/v1/auth/temporary-passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","name":"박기택"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void 이메일이나_이름이_맞지_않으면_404() throws Exception {
        when(temporaryPasswordService.issue(anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

        mockMvc.perform(post("/api/v1/auth/temporary-passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","name":"없는이름"}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 메일_발송에_실패하면_500_MAIL_SEND_FAILED() throws Exception {
        when(temporaryPasswordService.issue(anyString(), anyString()))
                .thenThrow(new BusinessException(ErrorCode.MAIL_SEND_FAILED));

        mockMvc.perform(post("/api/v1/auth/temporary-passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","name":"박기택"}"""))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("MAIL_SEND_FAILED"));
    }

    @Test
    void 임시_비밀번호_발급에_이메일_형식이_틀리면_400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/temporary-passwords")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-email","name":""}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors.length()").value(2));
        verifyNoInteractions(temporaryPasswordService);
    }

    @Test
    void 만료되거나_위조된_토큰이_붙어_와도_로그인은_된다() throws Exception {
        when(authService.login("user@example.com", "Passw0rd!")).thenReturn(
                new LoginResponse("token-value", 7200, new LoginResponse.UserSummary(1L, "기택"), false));

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer garbage.token.value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"Passw0rd!"}"""))
                .andExpect(status().isOk());
    }
}
