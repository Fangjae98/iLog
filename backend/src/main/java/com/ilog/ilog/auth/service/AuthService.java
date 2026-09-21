package com.ilog.ilog.auth.service;

import com.ilog.ilog.auth.dto.LoginResponse;
import com.ilog.ilog.global.auth.jwt.JwtTokenProvider;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 이메일·비밀번호가 맞으면 accessToken 을 발급한다.
     *
     * 없는 이메일, 틀린 비밀번호, 탈퇴 회원은 모두 같은 LOGIN_FAILED 로 응답해
     * 어느 쪽이 틀렸는지(계정 존재·탈퇴 여부)를 드러내지 않는다.
     * 탈퇴 30일 이내 계정 복구는 3단계 탈퇴 작업에서 다시 정한다.
     *
     * UserService 에 로그인용 조회(getActiveByEmail)가 아직 없어서 UserRepository 를 직접 쓴다.
     */
    @Transactional(readOnly = true)
    public LoginResponse login(String email, String rawPassword) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .filter(u -> passwordEncoder.matches(rawPassword, u.getPassword()))
                .filter(u -> !u.isWithdrawn())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.isTempPassword());
        return LoginResponse.of(accessToken, jwtTokenProvider.accessTokenValiditySeconds(), user);
    }

    /** UserService.normalizeEmail 과 같은 규칙 (D-16). 가입 때 소문자로 저장하므로 조회도 소문자로 한다. */
    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
