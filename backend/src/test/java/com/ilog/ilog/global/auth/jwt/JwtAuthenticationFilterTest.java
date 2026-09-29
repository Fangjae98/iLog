package com.ilog.ilog.global.auth.jwt;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.support.SecuredSliceTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bearer 토큰 → SecurityContext → {@code @Login LoginUser} 주입까지 실제 보안 체인으로 확인한다.
 *
 * <p>T08 에서 없앤 테스트 둘:
 * <ul>
 *   <li>{@code 토큰과_개발용_헤더가_함께_오면_토큰이_이긴다} — X-User-Id 헤더 자체가 사라져 검증할 대상이 없다</li>
 *   <li>{@code 공개_API는_무효한_토큰이_붙어도_막지_않는다} — {@code /api/v1/test/public} 은
 *       {@code anyRequest().authenticated()} 아래에서 더 이상 공개 경로가 아니다.
 *       이 의도는 실제 공개 경로인 {@code GET /api/v1/users/email-availability} 로 옮겼다
 *       ({@code UserControllerTest})</li>
 * </ul>
 */
@WebMvcTest(controllers = JwtAuthenticationFilterTest.TestController.class)
@Import(JwtAuthenticationFilterTest.TestController.class)
class JwtAuthenticationFilterTest extends SecuredSliceTestSupport {

    private static final String URL = "/api/v1/test/me";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtProperties jwtProperties;

    @Test
    void 유효한_토큰이면_토큰의_회원이_주입된다() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(7, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7))
                .andExpect(jsonPath("$.tempPassword").value(true));
    }

    @Test
    void Bearer_대소문자는_구분하지_않는다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(7L, false);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(7));
    }

    @Test
    void 탈퇴한_회원의_토큰은_401이다() throws Exception {
        // A6: 탈퇴해도 발급된 토큰은 만료까지 살아 있다. 토큰 저장소가 없어 요청마다 회원 상태를 본다.
        given(activeUserChecker.isActive(7L)).willReturn(false);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, bearer(7)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 위조된_토큰이면_401_UNAUTHORIZED() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer garbage.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 만료된_토큰이면_401_UNAUTHORIZED() throws Exception {
        JwtTokenProvider past = new JwtTokenProvider(jwtProperties,
                Clock.fixed(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC));
        String expired = past.createAccessToken(7L, false);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void Bearer가_아닌_인증_헤더는_무시한다() throws Exception {
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer "))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 토큰이_아예_없으면_401_JSON이다() throws Exception {
        // EntryPoint 경로. 톰캣 기본 HTML 오류 페이지가 아니라 공통 에러 형식이어야 한다 (지시서 3장).
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/test/me")
        LoginUser me(@Login LoginUser loginUser) {
            return loginUser;
        }
    }
}
