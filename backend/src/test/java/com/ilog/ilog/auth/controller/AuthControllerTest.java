package com.ilog.ilog.auth.controller;

import com.ilog.ilog.auth.dto.LoginResponse;
import com.ilog.ilog.auth.service.AuthService;
import com.ilog.ilog.global.config.SecurityConfig;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String URL = "/api/v1/auth/tokens";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AuthService authService;

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
