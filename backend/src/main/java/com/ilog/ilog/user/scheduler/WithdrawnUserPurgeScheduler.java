package com.ilog.ilog.user.scheduler;

import com.ilog.ilog.user.domain.UserPolicy;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * 탈퇴 후 30일이 지난 회원을 매일 물리 삭제한다 (U4, D-11).
 *
 * <p>행이 남아 있으면 이메일·닉네임 UNIQUE 제약 때문에 같은 값으로 다시 가입할 수 없다.
 * 지우고 나면 재가입 제한(U5, {@code REJOIN_RESTRICTED})도 자연히 풀린다.
 *
 * <p>DB FK 의 ON DELETE CASCADE 에만 기대지 않고 <b>자식 → 부모 순서로 직접 벌크 삭제</b>한다.
 * {@code ddl-auto=update} 는 이미 있는 FK 제약을 바꾸지 못해서, 예전에 만든 로컬 DB 에는 CASCADE 가 없을 수 있다.
 * 삭제는 한 트랜잭션이라 중간에 실패하면 모두 되돌아간다.
 *
 * <p>로그에는 건수만 남긴다. 이메일 같은 개인정보는 남기지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnUserPurgeScheduler {

    /** 이 조건에 걸리는 회원이 삭제 대상이다. 각 쿼리에서 같은 조건을 쓴다. */
    private static final String EXPIRED_USERS = "SELECT id FROM users WHERE withdrawn_at < :cutoff";
    private static final String EXPIRED_POSTS = "SELECT id FROM posts WHERE user_id IN (" + EXPIRED_USERS + ")";

    private final EntityManager entityManager;
    private final Clock clock;

    /** 매일 04:00 KST. 사용자가 가장 적은 시간대다. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    @Transactional
    public void purge() {
        LocalDateTime cutoff = LocalDateTime.now(clock).minusDays(UserPolicy.WITHDRAWAL_RECOVERY_DAYS);

        int urls = delete("DELETE FROM post_url WHERE post_id IN (" + EXPIRED_POSTS + ")", cutoff);
        int hashtags = delete("DELETE FROM post_hashtag WHERE post_id IN (" + EXPIRED_POSTS + ")", cutoff);
        int posts = delete("DELETE FROM posts WHERE user_id IN (" + EXPIRED_USERS + ")", cutoff);
        int histories = delete("DELETE FROM password_history WHERE user_id IN (" + EXPIRED_USERS + ")", cutoff);
        int users = delete("DELETE FROM users WHERE withdrawn_at < :cutoff", cutoff);

        log.info("탈퇴 30일 경과 회원 삭제: 회원 {}명, 게시글 {}건, 해시태그 {}건, URL {}건, 비밀번호 이력 {}건",
                users, posts, hashtags, urls, histories);
    }

    private int delete(String sql, LocalDateTime cutoff) {
        return entityManager.createNativeQuery(sql)
                .setParameter("cutoff", cutoff)
                .executeUpdate();
    }
}
