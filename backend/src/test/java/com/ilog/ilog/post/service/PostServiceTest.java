package com.ilog.ilog.post.service;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.dto.PostUpdateRequest;
import com.ilog.ilog.support.DatabaseTestSupport;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.service.UserService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 게시글 입력 규칙과 부분 수정 (T13 완료 기준 표, P1~P6).
 *
 * <p>해시태그 교체가 UNIQUE(post_id, name) 에 걸리지 않는지, 수정 일시가 실제로 저장되는지는
 * DB 에 쓰여야 알 수 있어서 PostgreSQL 에서 돈다.
 */
class PostServiceTest extends DatabaseTestSupport {

    @Autowired
    PostService postService;

    @Autowired
    UserService userService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    Long userId;

    @BeforeEach
    void 회원을_만든다() {
        jdbcTemplate.execute("TRUNCATE users, posts, post_url, post_hashtag, password_history CASCADE");
        userId = userService.signup(new SignupRequest("writer@example.com", "Passw0rd!", "박기택", "기택"));
    }

    // ---------- 작성 ----------

    @Test
    void urls_를_생략하면_빈_목록으로_저장된다() {
        PostResponse created = postService.create(userId, new PostCreateRequest("제목", "내용", null, null));

        PostResponse found = postService.getPost(created.id());
        assertThat(found.urls()).isEmpty();
        assertThat(found.hashtags()).isEmpty();
    }

    @Test
    void 제목과_내용의_앞뒤_공백은_지워서_저장한다() {
        PostResponse created = postService.create(userId, new PostCreateRequest("  제목 ", "\n내용\n", List.of(" https://a.com "), null));

        assertThat(created.title()).isEqualTo("제목");
        assertThat(created.content()).isEqualTo("내용");
        assertThat(created.urls()).containsExactly("https://a.com");
    }

    @Test
    void 해시태그는_샵과_공백을_지우고_소문자로_바꾼_뒤_중복을_없앤다() {
        PostResponse created = postService.create(userId,
                new PostCreateRequest("제목", "내용", null, List.of("#Spring", "spring", " JWT ")));

        assertThat(postService.getPost(created.id()).hashtags()).containsExactly("spring", "jwt");
    }

    @ParameterizedTest
    @ValueSource(strings = {"가나다라마바사아자차카타파하가나다라마바사", "#", "a-b", "C++"})
    void 다듬은_해시태그가_글자_규칙에_맞지_않으면_INVALID_INPUT(String tag) {
        assertBusiness(() -> postService.create(userId, new PostCreateRequest("제목", "내용", null, List.of(tag))),
                ErrorCode.INVALID_INPUT);
    }

    @Test
    void 해시태그_20자와_한글_영문_숫자_밑줄은_받는다() {
        PostResponse created = postService.create(userId,
                new PostCreateRequest("제목", "내용", null, List.of("가".repeat(20), "spring_boot4")));

        assertThat(created.hashtags()).containsExactly("가".repeat(20), "spring_boot4");
    }

    @Test
    void 다듬은_뒤_11개면_HASHTAG_LIMIT_EXCEEDED() {
        List<String> tags = IntStream.rangeClosed(1, 11).mapToObj(i -> "tag" + i).toList();

        assertBusiness(() -> postService.create(userId, new PostCreateRequest("제목", "내용", null, tags)),
                ErrorCode.HASHTAG_LIMIT_EXCEEDED);
    }

    @Test
    void 대소문자만_다른_태그는_하나로_세서_10개_제한을_넘지_않는다() {
        List<String> tags = IntStream.rangeClosed(1, 10)
                .boxed()
                .flatMap(i -> java.util.stream.Stream.of("tag" + i, "TAG" + i))
                .toList();

        PostResponse created = postService.create(userId, new PostCreateRequest("제목", "내용", null, tags));

        assertThat(created.hashtags()).hasSize(10);
    }

    // ---------- 수정 (PATCH) ----------

    @Test
    void 제목만_보내면_제목만_바뀌고_나머지는_유지된다() {
        Long postId = createFull();

        postService.update(userId, postId, new PostUpdateRequest("새 제목", null, null, null));

        PostResponse found = postService.getPost(postId);
        assertThat(found.title()).isEqualTo("새 제목");
        assertThat(found.content()).isEqualTo("내용");
        assertThat(found.urls()).containsExactly("https://a.com", "https://b.com");
        assertThat(found.hashtags()).containsExactly("spring", "jpa");
    }

    @Test
    void 해시태그를_빈_배열로_보내면_모두_지운다() {
        Long postId = createFull();

        postService.update(userId, postId, new PostUpdateRequest(null, null, null, List.of()));

        PostResponse found = postService.getPost(postId);
        assertThat(found.hashtags()).isEmpty();
        assertThat(found.urls()).hasSize(2);
    }

    @Test
    void URL을_보내면_보낸_목록으로_통째로_교체한다() {
        Long postId = createFull();

        postService.update(userId, postId, new PostUpdateRequest(null, null, List.of("https://c.com"), null));

        assertThat(postService.getPost(postId).urls()).containsExactly("https://c.com");
    }

    @Test
    void 같은_값을_그대로_보내도_수정_일시가_채워진다() {
        Long postId = createFull();

        postService.update(userId, postId,
                new PostUpdateRequest("제목", "내용", List.of("https://a.com", "https://b.com"), List.of("spring", "jpa")));

        PostResponse found = postService.getPost(postId);
        assertThat(found.updatedAt()).isNotNull();
        // 같은 태그를 다시 보내도 UNIQUE(post_id, name) 에 걸리지 않는다
        assertThat(found.hashtags()).containsExactly("spring", "jpa");
    }

    @Test
    void 일부만_겹치는_태그를_보내면_빠진_것은_지우고_새_것은_더한다() {
        Long postId = createFull();

        postService.update(userId, postId, new PostUpdateRequest(null, null, null, List.of("JPA", "docker")));

        assertThat(postService.getPost(postId).hashtags()).containsExactlyInAnyOrder("jpa", "docker");
    }

    @Test
    void 아무_필드도_보내지_않아도_수정_일시가_채워진다() {
        Long postId = createFull();

        postService.update(userId, postId, new PostUpdateRequest(null, null, null, null));

        assertThat(postService.getPost(postId).updatedAt()).isNotNull();
    }

    @Test
    void 수정에서도_해시태그_개수_제한을_지킨다() {
        Long postId = createFull();
        List<String> tags = IntStream.rangeClosed(1, 11).mapToObj(i -> "tag" + i).toList();

        assertBusiness(() -> postService.update(userId, postId, new PostUpdateRequest(null, null, null, tags)),
                ErrorCode.HASHTAG_LIMIT_EXCEEDED);
    }

    private Long createFull() {
        return postService.create(userId, new PostCreateRequest("제목", "내용",
                List.of("https://a.com", "https://b.com"), List.of("Spring", "#jpa"))).id();
    }

    private void assertBusiness(ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
