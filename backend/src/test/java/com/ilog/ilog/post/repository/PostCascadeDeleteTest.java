package com.ilog.ilog.post.repository;

import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.service.PostService;
import com.ilog.ilog.support.DatabaseTestSupport;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게시글 ↔ 회원 FK CASCADE (T09 완료 기준, P9).
 *
 * <p>회원 행을 JPA 를 거치지 않고 SQL 로 직접 지워서, DB 의 {@code ON DELETE CASCADE} 만으로
 * 글·태그·URL 이 함께 지워지는지 본다. 목으로는 확인할 수 없어 실제 PostgreSQL 에서 돈다.
 */
class PostCascadeDeleteTest extends DatabaseTestSupport {

    @Autowired
    UserService userService;

    @Autowired
    PostService postService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void 비운다() {
        jdbcTemplate.execute("TRUNCATE users, posts, post_url, post_hashtag, password_history CASCADE");
    }

    @Test
    void 회원_행을_지우면_글과_태그와_URL이_함께_지워진다() {
        Long writerId = userService.signup(new SignupRequest("writer@example.com", "Passw0rd!", "작성자", "작성자"));
        Long otherId = userService.signup(new SignupRequest("other@example.com", "Passw0rd!", "다른이", "다른이"));
        postService.create(writerId, new PostCreateRequest("제목", "내용", List.of("https://a.com"), List.of("spring")));
        postService.create(otherId, new PostCreateRequest("남의 글", "내용", List.of("https://b.com"), List.of("jpa")));

        jdbcTemplate.update("DELETE FROM users WHERE id = ?", writerId);

        assertThat(count("posts")).isEqualTo(1);
        assertThat(count("post_url")).isEqualTo(1);
        assertThat(count("post_hashtag")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT user_id FROM posts", Long.class)).isEqualTo(otherId);
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
