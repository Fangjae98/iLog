package com.ilog.ilog.user.service;

import com.ilog.ilog.user.domain.PasswordHistory;
import com.ilog.ilog.user.domain.UserPolicy;
import com.ilog.ilog.user.repository.PasswordHistoryRepository;
import com.ilog.ilog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 비밀번호 재사용 검사와 이력 보관 (U7, D-13).
 *
 * <p>{@link UserService} 와 분리된 별도 빈이다. {@code UserService.signup} 은 일부러
 * 트랜잭션을 걸지 않는데, 이력 저장은 트랜잭션 안에서 돌아야 한다.
 * 자기 자신을 호출하면 프록시를 타지 않아 {@code @Transactional} 이 조용히 무시되므로
 * 반드시 다른 빈이어야 한다.
 */
@Service
@RequiredArgsConstructor
public class PasswordHistoryService {

    private final PasswordHistoryRepository passwordHistoryRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 이력 1건을 남기고 한도를 넘은 오래된 것을 지운다.
     *
     * @param encodedPassword 이미 해싱된 비밀번호. 평문을 넘기지 말 것.
     */
    @Transactional
    public void record(Long userId, String encodedPassword) {
        // getReferenceById: FK 만 채우면 되므로 회원을 실제로 조회하지 않는다
        passwordHistoryRepository.save(
                new PasswordHistory(userRepository.getReferenceById(userId), encodedPassword));

        List<Long> keepIds = recentHistories(userId).stream()
                .map(PasswordHistory::getId)
                .toList();
        passwordHistoryRepository.deleteByUserIdAndIdNotIn(userId, keepIds);
    }

    /**
     * 최근 비밀번호를 다시 쓰려는지 검사한다 (U7: 최근 3개, <b>현재 포함</b>).
     *
     * <p>현재 해시를 함께 보는 이유: 이 테이블이 생기기 전에 가입한 회원은 이력 행이 없고,
     * 가입 직후 이력 저장에 실패하는 좁은 구간도 있다. 그때 현재 비밀번호로 "변경"이
     * 통과해 버리는 것을 막는다.
     *
     * @param currentEncoded 회원의 현재 비밀번호 해시
     * @param rawNewPassword 새로 쓰려는 평문 비밀번호
     */
    @Transactional(readOnly = true)
    public boolean isReused(Long userId, String currentEncoded, String rawNewPassword) {
        if (passwordEncoder.matches(rawNewPassword, currentEncoded)) {
            return true;
        }
        return recentHistories(userId).stream()
                .anyMatch(history -> passwordEncoder.matches(rawNewPassword, history.getPasswordHash()));
    }

    private List<PasswordHistory> recentHistories(Long userId) {
        return passwordHistoryRepository.findByUserIdOrderByIdDesc(
                userId, PageRequest.of(0, UserPolicy.PASSWORD_HISTORY_LIMIT));
    }
}
