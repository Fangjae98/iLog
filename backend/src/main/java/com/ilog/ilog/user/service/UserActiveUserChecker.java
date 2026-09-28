package com.ilog.ilog.user.service;

import com.ilog.ilog.global.auth.ActiveUserChecker;
import com.ilog.ilog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** {@link ActiveUserChecker} 의 회원 도메인 구현 (A6). */
@Component
@RequiredArgsConstructor
public class UserActiveUserChecker implements ActiveUserChecker {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long userId) {
        return userRepository.existsByIdAndWithdrawnAtIsNull(userId);
    }
}
