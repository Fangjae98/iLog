package com.ilog.ilog.auth.service;

import com.ilog.ilog.auth.dto.LoginResponse;
import com.ilog.ilog.global.auth.jwt.JwtTokenProvider;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.domain.UserPolicy;
import com.ilog.ilog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 이메일·비밀번호가 맞으면 accessToken 을 발급한다 (AUTH-01).
     *
     * <p><b>아래 순서가 곧 정책이다</b> (지시서 T04). 필터 체인으로 합치지 말 것.
     * <ol>
     *   <li>없는 이메일 → {@code LOGIN_FAILED}</li>
     *   <li>비밀번호 불일치 → {@code LOGIN_FAILED}. 1번과 구분하지 않는다</li>
     *   <li>탈퇴 30일 이내 → {@code USER_WITHDRAWN}(403) / 30일 경과 → {@code LOGIN_FAILED}</li>
     *   <li>임시 비밀번호 만료(24시간, U6) → {@code TEMP_PASSWORD_EXPIRED}(401)</li>
     *   <li>토큰 발급</li>
     * </ol>
     *
     * <p>2번이 3번보다 먼저인 것이 핵심이다 (U2). 비밀번호까지 맞은 사람에게만 탈퇴 사실을 알려 주고,
     * 이메일만 아는 사람에게는 계정 상태가 드러나지 않는다.
     *
     * <p>UserService 에 로그인용 조회(getActiveByEmail)가 아직 없어서 UserRepository 를 직접 쓴다.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        LocalDateTime now = LocalDateTime.now();

        if (user.isWithdrawn()) {
            boolean recoverable = user.getWithdrawnAt()
                    .plusDays(UserPolicy.WITHDRAWAL_RECOVERY_DAYS)
                    .isAfter(now);
            // 복구 기간이 지났으면 곧 물리 삭제될 계정이라 존재를 알리지 않는다
            throw new BusinessException(recoverable ? ErrorCode.USER_WITHDRAWN : ErrorCode.LOGIN_FAILED);
        }

        // 임시 비밀번호는 24시간만 유효하다 (U6). 만료되면 다시 발급받아야 한다.
        if (user.isTempPasswordExpired(now)) {
            throw new BusinessException(ErrorCode.TEMP_PASSWORD_EXPIRED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.isTempPassword());
        return LoginResponse.of(accessToken, jwtTokenProvider.accessTokenValiditySeconds(), user);
    }

    /** UserService.normalizeEmail 과 같은 규칙 (D-16). 가입 때 소문자로 저장하므로 조회도 소문자로 한다. */
    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
