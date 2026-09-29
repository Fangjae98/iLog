package com.ilog.ilog.post.service;

import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostPageResponse;
import com.ilog.ilog.post.dto.PostSearchRequest;
import com.ilog.ilog.support.DatabaseTestSupport;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.service.UserService;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 목록 조회의 SQL 실행 횟수 (T16-2 기대값: 본문 1 + 카운트 1 + 태그 IN 1 = 3회 이내).
 *
 * <p>작성자를 글마다 따로 조회하거나(N+1), 태그를 글마다 따로 조회하면 이 테스트가 깨진다.
 * 글 수가 늘어도 SQL 횟수가 그대로인지 보려고 쪽 크기(10)보다 많은 글을 만든다.
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PostSearchQueryCountTest extends DatabaseTestSupport {

    @Autowired
    PostService postService;

    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    Long me;

    @BeforeEach
    void 글을_만든다() {
        jdbcTemplate.execute("TRUNCATE users, posts, post_url, post_hashtag, password_history CASCADE");
        me = userService.signup(new SignupRequest("me@example.com", "Passw0rd!", "박기택", "기택"));
        Long other = userService.signup(new SignupRequest("other@example.com", "Passw0rd!", "다른이", "다른이"));
        for (int i = 0; i < 15; i++) {
            Long writer = i % 2 == 0 ? me : other;
            postService.create(writer, new PostCreateRequest("글" + i, "내용", null, List.of("spring", "jpa" + i)));
        }
    }

    @Test
    void 전체_목록은_작성자와_태그를_합쳐_SQL_3번_이내() {
        Statistics stats = statistics();

        PostPageResponse page = postService.search(me, request(null, null));

        assertThat(page.content()).hasSize(10);
        assertThat(page.content()).allSatisfy(post -> {
            assertThat(post.nickname()).isNotNull();
            assertThat(post.hashtags()).isNotEmpty();
        });
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void 태그_검색과_내_글도_SQL_3번_이내() {
        Statistics stats = statistics();

        postService.search(me, request(List.of("spring", "jpa1"), null));
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);

        stats.clear();
        postService.search(me, request(null, "me"));
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    @Test
    void 결과가_쪽_크기보다_적으면_카운트_쿼리를_생략한다() {
        Statistics stats = statistics();

        // 태그 검색 결과 1건 → 전체 개수를 이미 알 수 있어 count 를 실행하지 않는다 (본문 1 + 태그 IN 1)
        PostPageResponse page = postService.search(me, request(List.of("jpa3"), null));

        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(stats.getPrepareStatementCount()).isEqualTo(2);
    }

    private Statistics statistics() {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        return stats;
    }

    private PostSearchRequest request(List<String> hashtag, String author) {
        return new PostSearchRequest(null, null, null, hashtag, null, author, null, null);
    }
}
