package com.ilog.ilog.user.scheduler;

import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.service.PostService;
import com.ilog.ilog.support.DatabaseTestSupport;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 30일 경과 탈퇴 회원 삭제 (T12 완료 기준, U4).
 *
 * <p>벌크 삭제 SQL 이 실제로 도는지, 자식 행까지 지워지는지 봐야 해서 PostgreSQL 에서 돈다.
 */
class WithdrawnUserPurgeSchedulerTest extends DatabaseTestSupport {

    private static final String PASSWORD = "Passw0rd!";

    @Autowired
    WithdrawnUserPurgeScheduler scheduler;

    @Autowired
    UserService userService;

    @Autowired
    PostService postService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    Clock clock;

    @BeforeEach
    void 비운다() {
        jdbcTemplate.execute("TRUNCATE users, posts, post_url, post_hashtag, password_history CASCADE");
    }

    @Test
    void 탈퇴_31일차_회원과_그_글_태그_URL_이력은_지우고_29일차_회원은_남긴다() {
        Long expired = signupWithPost("old@example.com", "오래된");
        Long recent = signupWithPost("recent@example.com", "최근");
        Long active = signupWithPost("active@example.com", "활성");
        withdrawDaysAgo(expired, 31);
        withdrawDaysAgo(recent, 29);

        scheduler.purge();

        assertThat(jdbcTemplate.queryForList("SELECT id FROM users ORDER BY id", Long.class))
                .containsExactly(recent, active);
        assertThat(countWhere("posts", "user_id", expired)).isZero();
        assertThat(countWhere("password_history", "user_id", expired)).isZero();
        // 남은 두 회원의 글에 딸린 태그·URL 만 남는다
        assertThat(count("posts")).isEqualTo(2);
        assertThat(count("post_hashtag")).isEqualTo(2);
        assertThat(count("post_url")).isEqualTo(2);
        assertThat(count("password_history")).isEqualTo(2);
    }

    @Test
    void 삭제된_회원의_이메일과_닉네임으로_다시_가입할_수_있다() {
        Long expired = signupWithPost("old@example.com", "오래된");
        withdrawDaysAgo(expired, 31);

        scheduler.purge();

        Long rejoined = userService.signup(new SignupRequest("old@example.com", PASSWORD, "박기택", "오래된"));
        assertThat(rejoined).isNotEqualTo(expired);
    }

    private Long signupWithPost(String email, String nickname) {
        Long userId = userService.signup(new SignupRequest(email, PASSWORD, "박기택", nickname));
        postService.create(userId, new PostCreateRequest("제목", "내용", List.of("https://a.com"), List.of("spring")));
        return userId;
    }

    private void withdrawDaysAgo(Long userId, int days) {
        jdbcTemplate.update("UPDATE users SET withdrawn_at = ? WHERE id = ?",
                LocalDateTime.now(clock).minusDays(days), userId);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private int countWhere(String table, String column, Long value) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
    }
}
