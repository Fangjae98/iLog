package com.ilog.ilog;

import com.ilog.ilog.support.DatabaseTestSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 시나리오 (T17-1). 필터·보안·컨트롤러·서비스·DB 를 목 없이 한 번에 지나가며 사용자 흐름을 따라간다.
 *
 * <p>목으로 바꾼 것은 메일 발송({@link JavaMailSender}) 하나다. 보낸 메일 본문에서 임시 비밀번호를 꺼내 쓴다.
 * 실제 PostgreSQL 이 필요해서 Docker 가 없으면 건너뛴다.
 */
@AutoConfigureMockMvc
class ScenarioTest extends DatabaseTestSupport {

    private static final String EMAIL = "kitaek@example.com";
    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @MockitoBean
    JavaMailSender mailSender;

    @BeforeEach
    void 비운다() {
        jdbcTemplate.execute("TRUNCATE users, posts, post_url, post_hashtag, password_history CASCADE");
    }

    @Test
    void 가입_로그인_글작성_검색_로그아웃() throws Exception {
        signup();
        String token = login(PASSWORD).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String bearer = bearer(token);

        String created = mockMvc.perform(post("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"JPA 정리","content":"영속성 컨텍스트","urls":["https://a.com"],"hashtags":["#Spring"]}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andReturn().getResponse().getContentAsString();
        int postId = JsonPath.read(created, "$.postId");

        mockMvc.perform(get("/api/v1/posts").param("keyword", "jpa").param("hashtag", "spring")
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].postId").value(postId))
                .andExpect(jsonPath("$.content[0].nickname").value("기택"))
                .andExpect(jsonPath("$.content[0].hashtags[0]").value("spring"));

        mockMvc.perform(get("/api/v1/posts/" + postId).header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isMine").value(true))
                .andExpect(jsonPath("$.author.nickname").value("기택"));

        mockMvc.perform(delete("/api/v1/auth/tokens").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
    }

    @Test
    void 임시비밀번호로_로그인해서_비밀번호를_바꾸고_같은_토큰을_계속_쓴다() throws Exception {
        signup();

        mockMvc.perform(post("/api/v1/auth/temporary-passwords").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"name\":\"박기택\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("k***@example.com"));
        String temporaryPassword = sentTemporaryPassword();

        String tempLogin = login(temporaryPassword)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordResetRequired").value(true))
                .andReturn().getResponse().getContentAsString();
        String bearer = bearer(tempLogin);

        mockMvc.perform(put("/api/v1/users/me/password").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + temporaryPassword + "\",\"newPassword\":\"NewPassw0rd!\"}"))
                .andExpect(status().isNoContent());

        // A7·A8: 비밀번호를 바꿔도 로그인은 유지된다. tmp=true 가 남은 같은 토큰으로 계속 쓴다
        mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/auth/tokens").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNoContent());
        login(temporaryPassword).andExpect(status().isUnauthorized());
        login("NewPassw0rd!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.passwordResetRequired").value(false));
    }

    @Test
    void 탈퇴하면_로그인이_403이고_복구하면_다시_로그인된다() throws Exception {
        signup();
        String bearer = bearer(login(PASSWORD).andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/v1/users/me/withdrawal").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recoverableUntil").exists());

        // A6: 탈퇴 직후 남은 토큰은 막힌다
        mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isUnauthorized());
        login(PASSWORD)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("USER_WITHDRAWN"));

        mockMvc.perform(post("/api/v1/auth/account-recoveries").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isNoContent());

        login(PASSWORD).andExpect(status().isOk());
    }

    private void signup() throws Exception {
        mockMvc.perform(post("/api/v1/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + PASSWORD
                                + "\",\"name\":\"박기택\",\"nickname\":\"기택\"}"))
                .andExpect(status().isCreated());
    }

    private ResultActions login(String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/tokens").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + EMAIL + "\",\"password\":\"" + password + "\"}"));
    }

    private String bearer(String loginResponse) {
        return "Bearer " + JsonPath.read(loginResponse, "$.accessToken");
    }

    /** 메일 본문에서 들여쓴 한 줄(임시 비밀번호)을 꺼낸다. */
    private String sentTemporaryPassword() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        Matcher matcher = Pattern.compile("(?m)^ {4}(\\S+)$").matcher(captor.getValue().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }
}
