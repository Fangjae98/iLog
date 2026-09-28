package com.ilog.ilog.global.auth.jwt;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.global.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Bearer 토큰 → SecurityContext → @Login LoginUser 주입까지 실제 보안 체인으로 확인한다. */
@WebMvcTest(controllers = JwtAuthenticationFilterTest.TestController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilterTest.TestController.class})
class JwtAuthenticationFilterTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    JwtProperties jwtProperties;

    @Test
    void 유효한_토큰이면_토큰의_회원이_주입된다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(7L, true);

        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.tempPassword").value(true));
    }

    @Test
    void Bearer_대소문자는_구분하지_않는다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(7L, false);

        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7));
    }

    @Test
    void 토큰과_개발용_헤더가_함께_오면_토큰이_이긴다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(7L, false);

        mockMvc.perform(get("/api/v1/test/me")
                        .header("Authorization", "Bearer " + token)
                        .header("X-User-Id", "999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7));
    }

    @Test
    void 위조된_토큰이면_401_UNAUTHORIZED() throws Exception {
        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 만료된_토큰이면_401_UNAUTHORIZED() throws Exception {
        JwtTokenProvider past = new JwtTokenProvider(jwtProperties, Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC));
        String expired = past.createAccessToken(7L, false);

        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void Bearer가_아닌_인증_헤더는_무시한다() throws Exception {
        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 공개_API는_무효한_토큰이_붙어도_막지_않는다() throws Exception {
        mockMvc.perform(get("/api/v1/test/public").header("Authorization", "Bearer garbage.token.value"))
                .andExpect(status().isOk());
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/test/me")
        LoginUser me(@Login LoginUser loginUser) {
            return loginUser;
        }

        @GetMapping("/api/v1/test/public")
        String open() {
            return "ok";
        }
    }
}
