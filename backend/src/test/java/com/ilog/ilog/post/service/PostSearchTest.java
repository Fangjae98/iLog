package com.ilog.ilog.post.service;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostPageResponse;
import com.ilog.ilog.post.dto.PostSearchRequest;
import com.ilog.ilog.post.dto.PostSummaryResponse;
import com.ilog.ilog.support.DatabaseTestSupport;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 게시글 목록·검색·내 글 (T15 완료 기준, S1~S5).
 *
 * <p>ILIKE, 관계 나눗셈 서브쿼리, 날짜 경계는 실제 PostgreSQL 에서만 확인할 수 있다.
 */
class PostSearchTest extends DatabaseTestSupport {

    @Autowired
    PostService postService;

    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long me;
    Long other;

    @BeforeEach
    void 회원을_만든다() {
        jdbcTemplate.execute("TRUNCATE users, posts, post_url, post_hashtag, password_history CASCADE");
        me = userService.signup(new SignupRequest("me@example.com", "Passw0rd!", "박기택", "기택"));
        other = userService.signup(new SignupRequest("other@example.com", "Passw0rd!", "다른이", "다른이"));
    }

    // ---------- 조건 하나씩 ----------

    @Test
    void 조건이_없으면_전체를_최신순으로_돌려준다() {
        Long first = write(me, "첫 글", "내용");
        Long second = write(other, "둘째 글", "내용");
        Long third = write(me, "셋째 글", "내용");

        PostPageResponse page = search(condition());

        assertThat(ids(page)).containsExactly(third, second, first);
        assertThat(page.totalElements()).isEqualTo(3);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.content().getFirst().nickname()).isEqualTo("기택");
    }

    @Test
    void 작성_시각이_같으면_번호가_큰_글이_먼저다() {
        Long a = write(me, "a", "내용");
        Long b = write(me, "b", "내용");
        jdbcTemplate.update("UPDATE posts SET created_at = ?", LocalDateTime.of(2026, 9, 16, 10, 0));

        assertThat(ids(search(condition()))).containsExactly(b, a);
    }

    @Test
    void keyword는_제목_또는_본문에서_대소문자_없이_찾는다() {
        Long inTitle = write(me, "Spring JPA 정리", "내용");
        Long inContent = write(me, "제목", "오늘은 jpa 공부");
        write(me, "무관", "무관");

        assertThat(ids(search(condition().keyword("JPA")))).containsExactlyInAnyOrder(inTitle, inContent);
    }

    @Test
    void title은_제목에서만_찾는다() {
        Long inTitle = write(me, "스프링 시큐리티", "내용");
        write(me, "제목", "스프링 본문");

        assertThat(ids(search(condition().title("스프링")))).containsExactly(inTitle);
    }

    @Test
    void 검색어의_퍼센트와_밑줄은_글자_그대로_찾는다() {
        Long percent = write(me, "달성률 100%", "내용");
        write(me, "100점", "내용");
        Long underscore = write(me, "snake_case", "내용");
        write(me, "snakeXcase", "내용");

        assertThat(ids(search(condition().keyword("100%")))).containsExactly(percent);
        assertThat(ids(search(condition().keyword("e_c")))).containsExactly(underscore);
    }

    @Test
    void nickname은_정확히_같은_작성자의_글만() {
        Long mine = write(me, "내 글", "내용");
        write(other, "남의 글", "내용");

        assertThat(ids(search(condition().nickname("기택")))).containsExactly(mine);
        assertThat(ids(search(condition().nickname("기")))).isEmpty();
    }

    @Test
    void author_me는_로그인한_회원의_글만() {
        Long mine = write(me, "내 글", "내용");
        write(other, "남의 글", "내용");

        assertThat(ids(search(condition().author("me")))).containsExactly(mine);
    }

    @Test
    void 해시태그를_여러_개_보내면_모두_붙은_글만() {
        Long both = write(me, "둘 다", "내용", "spring", "jwt");
        write(me, "spring 만", "내용", "spring");
        write(me, "jwt 만", "내용", "jwt");
        Long more = write(me, "셋", "내용", "spring", "jwt", "jpa");

        assertThat(ids(search(condition().hashtag("spring", "jwt")))).containsExactlyInAnyOrder(both, more);
    }

    @Test
    void 해시태그는_저장과_같이_다듬어서_찾는다() {
        Long spring = write(me, "글", "내용", "spring");

        assertThat(ids(search(condition().hashtag("#Spring")))).containsExactly(spring);
        // 다듬은 뒤 빈 값("#")은 조건에서 뺀다
        assertThat(ids(search(condition().hashtag("#", " ")))).containsExactly(spring);
    }

    @Test
    void 검색_해시태그가_10개를_넘으면_INVALID_INPUT() {
        String[] tags = IntStream.rangeClosed(1, 11).mapToObj(i -> "tag" + i).toArray(String[]::new);

        assertThatThrownBy(() -> search(condition().hashtag(tags)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void date는_그날_00시부터_23시59분59초까지_포함하고_다음날_00시는_뺀다() {
        Long start = writeAt("시작", LocalDateTime.of(2026, 9, 16, 0, 0, 0));
        Long end = writeAt("끝", LocalDateTime.of(2026, 9, 16, 23, 59, 59));
        writeAt("전날", LocalDateTime.of(2026, 9, 15, 23, 59, 59));
        writeAt("다음날", LocalDateTime.of(2026, 9, 17, 0, 0, 0));

        assertThat(ids(search(condition().date(LocalDate.of(2026, 9, 16))))).containsExactly(end, start);
    }

    // ---------- 조합, 탈퇴, 쪽 ----------

    @Test
    void 조건을_여러_개_보내면_모두_만족하는_글만() {
        Long hit = write(me, "JPA 정리", "내용", "spring");
        write(me, "JPA 정리", "내용", "docker");
        write(other, "JPA 정리", "내용", "spring");

        assertThat(ids(search(condition().keyword("jpa").hashtag("spring").author("me")))).containsExactly(hit);
    }

    @Test
    void 탈퇴한_작성자의_글은_어떤_조건에서도_빠진다() {
        Long mine = write(me, "내 글", "내용", "spring");
        write(other, "남의 글", "내용", "spring");
        userService.withdraw(other, "Passw0rd!");

        assertThat(ids(search(condition()))).containsExactly(mine);
        assertThat(ids(search(condition().hashtag("spring")))).containsExactly(mine);
        assertThat(ids(search(condition().nickname("다른이")))).isEmpty();
    }

    @Test
    void 쪽을_나누고_다음_쪽_여부를_알려준다() {
        List<Long> written = IntStream.range(0, 12).mapToObj(i -> write(me, "글" + i, "내용")).toList();

        PostPageResponse first = search(condition().size(5));
        PostPageResponse last = search(condition().size(5).page(2));

        assertThat(first.content()).hasSize(5);
        assertThat(first.totalElements()).isEqualTo(12);
        assertThat(first.totalPages()).isEqualTo(3);
        assertThat(first.hasNext()).isTrue();
        assertThat(ids(last)).containsExactly(written.get(1), written.get(0));
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    void 결과가_없으면_빈_목록() {
        PostPageResponse page = search(condition().keyword("없는말"));

        assertThat(page.content()).isEmpty();
        assertThat(page.totalElements()).isZero();
    }

    // ---------- 도우미 ----------

    private Long write(Long userId, String title, String content, String... tags) {
        return postService.create(userId, new PostCreateRequest(title, content, null, List.of(tags))).postId();
    }

    private Long writeAt(String title, LocalDateTime createdAt) {
        Long postId = write(me, title, "내용");
        jdbcTemplate.update("UPDATE posts SET created_at = ? WHERE id = ?", createdAt, postId);
        return postId;
    }

    private PostPageResponse search(Condition condition) {
        return postService.search(me, condition.build());
    }

    private List<Long> ids(PostPageResponse page) {
        return page.content().stream().map(PostSummaryResponse::postId).toList();
    }

    private Condition condition() {
        return new Condition();
    }

    /** 테스트에서 조건을 읽기 쉽게 조립하는 작은 빌더. */
    private static class Condition {
        private String keyword;
        private String title;
        private String nickname;
        private List<String> hashtag;
        private LocalDate date;
        private String author;
        private Integer page;
        private Integer size;

        Condition keyword(String value) { this.keyword = value; return this; }
        Condition title(String value) { this.title = value; return this; }
        Condition nickname(String value) { this.nickname = value; return this; }
        Condition hashtag(String... values) { this.hashtag = List.of(values); return this; }
        Condition date(LocalDate value) { this.date = value; return this; }
        Condition author(String value) { this.author = value; return this; }
        Condition page(int value) { this.page = value; return this; }
        Condition size(int value) { this.size = value; return this; }

        PostSearchRequest build() {
            return new PostSearchRequest(keyword, title, nickname, hashtag, date, author, page, size);
        }
    }
}
