package com.ilog.ilog.user.service;

import com.ilog.ilog.support.DatabaseTestSupport;
import com.ilog.ilog.user.domain.UserPolicy;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.repository.PasswordHistoryRepository;
import com.ilog.ilog.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 비밀번호 이력의 저장·정리 동작 (T05 완료 기준).
 *
 * <p>실제 PostgreSQL 이 필요하다. 이력 개수 제한은 삭제 쿼리가 도는지까지 봐야 하는데,
 * 목으로는 확인할 수 없기 때문이다. Docker 가 없으면 건너뛴다.
 */
class PasswordHistoryServiceTest extends DatabaseTestSupport {

    private static final String RAW_PASSWORD = "Passw0rd!";

    @Autowired
    UserService userService;

    @Autowired
    PasswordHistoryService passwordHistoryService;

    @Autowired
    PasswordHistoryRepository passwordHistoryRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @BeforeEach
    void 비운다() {
        passwordHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void 가입하면_이력이_1건_생긴다() {
        Long userId = userService.signup(
                new SignupRequest("user@example.com", RAW_PASSWORD, "박기택", "기택"));

        assertThat(passwordHistoryRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, 10)))
                .hasSize(1)
                .allSatisfy(history ->
                        assertThat(passwordEncoder.matches(RAW_PASSWORD, history.getPasswordHash())).isTrue());
    }

    @Test
    void 이력은_최신_3건만_남는다() {
        Long userId = userService.signup(
                new SignupRequest("user@example.com", RAW_PASSWORD, "박기택", "기택"));

        // 가입분 1건 + 3건 = 4건을 넣으면 오래된 것부터 잘려 3건이 된다
        passwordHistoryService.record(userId, passwordEncoder.encode("Second0!"));
        passwordHistoryService.record(userId, passwordEncoder.encode("Third00!"));
        passwordHistoryService.record(userId, passwordEncoder.encode("Fourth0!"));

        var remaining = passwordHistoryRepository.findByUserIdOrderByIdDesc(userId, PageRequest.of(0, 10));
        assertThat(remaining).hasSize(UserPolicy.PASSWORD_HISTORY_LIMIT);
        // 가장 오래된 것(가입 때 비밀번호)이 밀려났다
        assertThat(remaining).noneSatisfy(history ->
                assertThat(passwordEncoder.matches(RAW_PASSWORD, history.getPasswordHash())).isTrue());
    }

    @Test
    void 최근_3건_안에_있으면_재사용으로_본다() {
        Long userId = userService.signup(
                new SignupRequest("user@example.com", RAW_PASSWORD, "박기택", "기택"));
        String current = userRepository.findById(userId).orElseThrow().getPassword();

        // 현재 비밀번호 (U7 "현재 포함")
        assertThat(passwordHistoryService.isReused(userId, current, RAW_PASSWORD)).isTrue();
        // 쓴 적 없는 비밀번호
        assertThat(passwordHistoryService.isReused(userId, current, "Brandnew1!")).isFalse();
    }

    @Test
    void 한도_밖으로_밀려난_비밀번호는_다시_쓸_수_있다() {
        Long userId = userService.signup(
                new SignupRequest("user@example.com", RAW_PASSWORD, "박기택", "기택"));
        passwordHistoryService.record(userId, passwordEncoder.encode("Second0!"));
        passwordHistoryService.record(userId, passwordEncoder.encode("Third00!"));
        passwordHistoryService.record(userId, passwordEncoder.encode("Fourth0!"));

        String current = passwordEncoder.encode("Fourth0!");
        assertThat(passwordHistoryService.isReused(userId, current, RAW_PASSWORD)).isFalse();
    }
}
