package com.ilog.ilog.post.service;

import com.ilog.ilog.post.dto.PostSearchRequest;
import com.ilog.ilog.support.DatabaseTestSupport;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 목록·검색 쿼리 측정 (T16). 인덱스 적용 전후를 같은 조건으로 비교하기 위한 도구다.
 *
 * <p>더미 데이터(회원 20명, 글 1만 건, 글당 태그 0~5개)를 넣고 5가지 조회에 대해
 * SQL 실행 횟수(Hibernate Statistics), 서비스 호출 시간(중앙값), {@code EXPLAIN (ANALYZE, BUFFERS)} 를 남긴다.
 * 같은 데이터에서 T16 인덱스를 지운 상태(적용 전)와 다시 만든 상태(적용 후)를 차례로 잰다.
 * 결과는 {@code build/reports/measure/post-query.md} 에 쓴다.
 *
 * <p>데이터를 만들고 지우는 데 시간이 걸려 평소 테스트에서는 돌지 않는다.
 * {@code ILOG_MEASURE=true ./gradlew test --tests '*PostQueryMeasurementTest'} 로 실행한다.
 * 수치는 로컬 Testcontainers(PostgreSQL 16) 기준이다.
 */
@EnabledIfEnvironmentVariable(named = "ILOG_MEASURE", matches = "true")
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class PostQueryMeasurementTest extends DatabaseTestSupport {

    private static final String SELECT_COLUMNS = """
            SELECT p.id, p.content, p.created_at, p.title, p.updated_at, p.user_id,
                   u.id, u.created_at, u.email, u.name, u.nickname, u.password,
                   u.temp_password, u.temp_password_expires_at, u.updated_at, u.withdrawn_at
            FROM posts p JOIN users u ON u.id = p.user_id
            """;
    private static final String ORDER = " ORDER BY p.created_at DESC, p.id DESC ";

    @Autowired
    PostService postService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    EntityManagerFactory entityManagerFactory;

    /** T16 에서 추가한 인덱스. 적용 전 수치를 재려고 지웠다가 같은 정의로 다시 만든다. */
    private static final List<String> CREATE_INDEXES = List.of(
            "CREATE INDEX IF NOT EXISTS idx_posts_created ON posts (created_at DESC, id DESC)",
            "CREATE INDEX IF NOT EXISTS idx_posts_user_created ON posts (user_id, created_at DESC, id DESC)",
            "CREATE INDEX IF NOT EXISTS idx_posts_title_trgm ON posts USING gin (title gin_trgm_ops)",
            "CREATE INDEX IF NOT EXISTS idx_posts_content_trgm ON posts USING gin (content gin_trgm_ops)");

    @Test
    void 측정() throws IOException {
        seed();
        Long heavyUser = jdbcTemplate.queryForObject(
                "SELECT user_id FROM posts GROUP BY user_id ORDER BY COUNT(*) DESC, user_id LIMIT 1", Long.class);

        StringBuilder report = new StringBuilder("# 게시글 목록·검색 측정\n\n");
        report.append(environment()).append('\n');
        StringBuilder plans = new StringBuilder();

        for (String index : List.of("idx_posts_created", "idx_posts_user_created", "idx_posts_title_trgm", "idx_posts_content_trgm")) {
            jdbcTemplate.execute("DROP INDEX IF EXISTS " + index);
        }
        jdbcTemplate.execute("ANALYZE posts");
        measure("적용 전", heavyUser, report, plans);

        CREATE_INDEXES.forEach(jdbcTemplate::execute);
        jdbcTemplate.execute("ANALYZE posts");
        measure("적용 후", heavyUser, report, plans);

        Path out = Path.of("build/reports/measure/post-query.md");
        Files.createDirectories(out.getParent());
        Files.writeString(out, report.append("\n## 실행 계획\n").append(plans).toString());
        System.out.println("측정 결과: " + out.toAbsolutePath());
    }

    private void measure(String label, Long heavyUser, StringBuilder report, StringBuilder plans) {
        report.append("\n### ").append(label).append("\n\n")
                .append("| 조회 | SQL 횟수 | 서비스 호출 중앙값(ms) | 본문 쿼리 실행(ms) | count 쿼리 실행(ms) |\n")
                .append("|---|---|---|---|---|\n");

        Map<String, Scenario> scenarios = new LinkedHashMap<>();
        scenarios.put("전체 목록 1페이지", new Scenario(
                request(null, null, null, 0),
                "WHERE u.withdrawn_at IS NULL"));
        scenarios.put("전체 목록 500페이지", new Scenario(
                request(null, null, null, 499),
                "WHERE u.withdrawn_at IS NULL"));
        scenarios.put("내 글 1페이지", new Scenario(
                request(null, null, "me", 0),
                "WHERE u.withdrawn_at IS NULL AND p.user_id = " + heavyUser));
        scenarios.put("태그 2개 AND", new Scenario(
                request(null, List.of("spring", "jpa"), null, 0),
                """
                WHERE u.withdrawn_at IS NULL AND p.id IN (
                    SELECT h.post_id FROM post_hashtag h WHERE h.name IN ('spring', 'jpa')
                    GROUP BY 1 HAVING COUNT(h.id) = 2)"""));
        scenarios.put("키워드 3글자, 흔함(쿠버네, 20%)", new Scenario(
                request("쿠버네", null, null, 0),
                keywordWhere("쿠버네")));
        scenarios.put("키워드 3글자, 드묾(엘라스, 0.1%)", new Scenario(
                request("엘라스", null, null, 0),
                keywordWhere("엘라스")));

        for (var entry : scenarios.entrySet()) {
            String name = entry.getKey();
            PostSearchRequest request = entry.getValue().request();
            int offset = request.page() * request.size();

            long statements = countStatements(() -> postService.search(heavyUser, request));
            double medianMs = medianMillis(() -> postService.search(heavyUser, request));
            String selectSql = SELECT_COLUMNS + entry.getValue().where() + ORDER + " OFFSET " + offset + " LIMIT " + request.size();
            String countSql = "SELECT COUNT(p.id) FROM posts p JOIN users u ON u.id = p.user_id " + entry.getValue().where();
            String selectPlan = explain(selectSql);
            String countPlan = explain(countSql);

            report.append("| ").append(name)
                    .append(" | ").append(statements)
                    .append(" | ").append(String.format("%.1f", medianMs))
                    .append(" | ").append(executionTime(selectPlan))
                    .append(" | ").append(executionTime(countPlan))
                    .append(" |\n");
            plans.append("\n### ").append(label).append(" · ").append(name).append("\n\n```\n").append(selectPlan)
                    .append("\n-- count\n").append(countPlan).append("\n```\n");
        }
    }

    private String keywordWhere(String keyword) {
        return "WHERE u.withdrawn_at IS NULL AND (p.title ILIKE '%" + keyword + "%' ESCAPE '\\' "
                + "OR p.content ILIKE '%" + keyword + "%' ESCAPE '\\')";
    }

    /** 회원 20명, 글 1만 건(약 1년에 걸쳐), 글당 태그 0~5개. 한글 제목·본문을 섞는다. */
    private void seed() {
        jdbcTemplate.execute("TRUNCATE users, posts, post_url, post_hashtag, password_history CASCADE");
        jdbcTemplate.execute("""
                INSERT INTO users (email, nickname, password, name, temp_password, created_at)
                SELECT 'user' || g || '@example.com', '회원' || g, repeat('x', 60), '이름', false, now()
                FROM generate_series(1, 20) g""");
        jdbcTemplate.execute("""
                INSERT INTO posts (user_id, title, content, created_at)
                SELECT u.ids[1 + (g % 20)],
                       (ARRAY['스프링 부트 정리', 'JPA 영속성', '자바 스트림', '도커 입문',
                              '리액트 훅', '알고리즘 풀이', '데이터베이스 인덱스', '네트워크 기초'])[1 + (g % 8)] || ' ' || g,
                       repeat('오늘 공부한 내용을 정리한다. ', 5)
                           || (ARRAY['스프링', '트랜잭션', '쿠버네티스', '테스트 코드', '캐시'])[1 + ((g / 7) % 5)]
                           || CASE WHEN g % 1000 = 0 THEN ' 엘라스틱서치' ELSE '' END
                           || ' ' || md5(g::text),
                       timestamp '2025-10-01' + g * interval '50 minutes'
                FROM generate_series(1, 10000) g,
                     (SELECT array_agg(id ORDER BY id) AS ids FROM users) u""");
        jdbcTemplate.execute("""
                INSERT INTO post_hashtag (post_id, name)
                SELECT DISTINCT p.id,
                       (ARRAY['spring', 'jpa', 'java', 'docker', 'react', 'algo', 'db', 'network', 'test', 'cache',
                              'k8s', 'aws', 'linux', 'git', 'http', 'security', 'kotlin', 'python', 'redis', 'kafka'])
                           [(1 + (p.id * (k + 3) + k * k) % 20)::int]
                FROM posts p, generate_series(1, (p.id % 6)::int) k""");
        jdbcTemplate.execute("ANALYZE users; ANALYZE posts; ANALYZE post_hashtag");
    }

    private String environment() {
        String ctype = jdbcTemplate.queryForObject(
                "SELECT datctype FROM pg_database WHERE datname = current_database()", String.class);
        String trigram;
        try {
            jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm");
            trigram = jdbcTemplate.queryForObject("SELECT show_trgm('스프링')::text", String.class);
        } catch (Exception e) {
            trigram = "pg_trgm 사용 불가: " + e.getMessage();
        }
        String indexes = String.join(", ", jdbcTemplate.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename IN ('posts', 'post_hashtag') ORDER BY indexname",
                String.class));
        return "- 데이터: 회원 " + count("users") + "명, 글 " + count("posts") + "건, 태그 " + count("post_hashtag") + "건\n"
                + "- LC_CTYPE: " + ctype + "\n"
                + "- show_trgm('스프링'): " + trigram + "\n"
                + "- 인덱스: " + indexes + "\n";
    }

    private long countStatements(Runnable call) {
        Statistics stats = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        stats.clear();
        call.run();
        return stats.getPrepareStatementCount();
    }

    /** 첫 호출(캐시 데우기)을 버리고 7번의 중앙값. */
    private double medianMillis(Runnable call) {
        call.run();
        double[] times = new double[7];
        for (int i = 0; i < times.length; i++) {
            long start = System.nanoTime();
            call.run();
            times[i] = (System.nanoTime() - start) / 1_000_000.0;
        }
        Arrays.sort(times);
        return times[times.length / 2];
    }

    private String explain(String sql) {
        return String.join("\n", jdbcTemplate.queryForList("EXPLAIN (ANALYZE, BUFFERS) " + sql, String.class));
    }

    private String executionTime(String plan) {
        return plan.lines()
                .filter(line -> line.startsWith("Execution Time"))
                .map(line -> line.replace("Execution Time: ", "").replace(" ms", ""))
                .findFirst()
                .orElse("-");
    }

    private int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private PostSearchRequest request(String keyword, List<String> hashtag, String author, int page) {
        return new PostSearchRequest(keyword, null, null, hashtag, null, author, page, null);
    }

    private record Scenario(PostSearchRequest request, String where) {
    }
}
