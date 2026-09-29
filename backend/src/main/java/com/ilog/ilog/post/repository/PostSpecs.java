package com.ilog.ilog.post.repository;

import com.ilog.ilog.post.entity.Post;
import com.ilog.ilog.post.entity.PostHashtag;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 목록·검색 조건 조각 (T15, S3).
 *
 * <p>조건마다 {@link Specification} 하나를 만들고, 서비스가 받은 조건만 골라 AND 로 묶는다.
 * 조건 조합이 많아도 메서드 이름 쿼리를 늘리지 않아도 되고, 새 의존성(QueryDSL 등)도 필요 없다.
 *
 * <p>부분 일치는 PostgreSQL 의 {@code ILIKE} 로 만든다. {@code lower(title) like ...} 로 쓰면
 * T16 의 trigram 인덱스를 타지 않는다. JPQL 에는 ILIKE 가 없어서 Hibernate 전용 {@code ilike} 를 쓴다.
 */
public final class PostSpecs {

    /** LIKE 패턴에서 %, _ 를 글자 그대로 찾게 하는 이스케이프 문자. */
    private static final char ESCAPE = '\\';

    private PostSpecs() {
    }

    /** 작성자가 탈퇴하지 않은 글만 (P8). 목록·검색에 항상 붙는다. */
    public static Specification<Post> authorActive() {
        return (root, query, cb) -> cb.isNull(root.get("user").get("withdrawnAt"));
    }

    /** 제목 또는 본문에 keyword 가 들어간 글. */
    public static Specification<Post> keyword(String keyword) {
        return (root, query, cb) -> {
            HibernateCriteriaBuilder hcb = (HibernateCriteriaBuilder) cb;
            String pattern = containsPattern(keyword);
            return cb.or(
                    hcb.ilike(root.get("title"), pattern, ESCAPE),
                    hcb.ilike(root.get("content"), pattern, ESCAPE));
        };
    }

    /** 제목에 title 이 들어간 글. */
    public static Specification<Post> titleContains(String title) {
        return (root, query, cb) ->
                ((HibernateCriteriaBuilder) cb).ilike(root.get("title"), containsPattern(title), ESCAPE);
    }

    /** 작성자 닉네임이 정확히 같은 글. */
    public static Specification<Post> nickname(String nickname) {
        return (root, query, cb) -> cb.equal(root.get("user").get("nickname"), nickname);
    }

    /** 작성자가 이 회원인 글 (author=me). user_id 컬럼만 보므로 users 조인이 없다. */
    public static Specification<Post> writtenBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** 이 날짜(KST) 00:00 이상, 다음 날 00:00 미만에 작성된 글. 인덱스를 타도록 컬럼에 함수를 씌우지 않는다. */
    public static Specification<Post> createdOn(LocalDate date) {
        return (root, query, cb) -> {
            Expression<LocalDateTime> createdAt = root.get("createdAt");
            return cb.and(
                    cb.greaterThanOrEqualTo(createdAt, date.atStartOfDay()),
                    cb.lessThan(createdAt, date.plusDays(1).atStartOfDay()));
        };
    }

    /**
     * 받은 태그가 <b>모두</b> 붙은 글 (S3, AND).
     *
     * <p>관계 나눗셈: 태그 중 하나라도 붙은 행을 글별로 묶어, 개수가 받은 태그 수와 같은 글만 남긴다.
     * <pre>
     * p.id IN (SELECT h.post_id FROM post_hashtag h WHERE h.name IN (:tags)
     *          GROUP BY h.post_id HAVING COUNT(*) = :tagCount)
     * </pre>
     * UNIQUE(post_id, name) 이라 한 글에 같은 태그가 두 번 세어지지 않아 COUNT(*) 로 충분하다.
     * 태그마다 EXISTS 를 붙이면 태그 수만큼 서브쿼리가 늘고, JOIN 으로 걸면 글이 중복돼 쪽 계산이 틀어진다.
     *
     * @param tags 다듬고 중복을 없앤 태그 (빈 목록이면 부르지 않는다)
     */
    public static Specification<Post> hasAllTags(List<String> tags) {
        return (root, query, cb) -> {
            Subquery<Long> sub = query.subquery(Long.class);
            Root<PostHashtag> h = sub.from(PostHashtag.class);
            sub.select(h.get("post").get("id"))
                    .where(h.get("name").in(tags))
                    .groupBy(h.get("post").get("id"))
                    .having(cb.equal(cb.count(h), (long) tags.size()));
            return root.get("id").in(sub);
        };
    }

    /** "스프링%" 처럼 사용자가 넣은 %, _ 를 글자 그대로 찾도록 이스케이프하고 앞뒤에 % 를 붙인다. */
    static String containsPattern(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
