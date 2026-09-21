package com.ilog.ilog.auth.service;

import com.ilog.ilog.auth.dto.LoginResponse;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.global.auth.jwt.JwtProperties;
import com.ilog.ilog.global.auth.jwt.JwtTokenProvider;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String RAW_PASSWORD = "Passw0rd!";

    @Mock
    UserRepository userRepository;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(
            new JwtProperties("test-only-jwt-secret-0123456789-abcdefghij", Duration.ofHours(2)));

    AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void 이메일과_비밀번호가_맞으면_토큰과_회원_정보를_돌려준다() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L)));

        LoginResponse response = authService.login(EMAIL, RAW_PASSWORD);

        assertThat(jwtTokenProvider.parse(response.accessToken())).contains(new LoginUser(1L, false));
        assertThat(response.expiresIn()).isEqualTo(7200);
        assertThat(response.user().userId()).isEqualTo(1L);
        assertThat(response.user().nickname()).isEqualTo("기택");
        assertThat(response.passwordResetRequired()).isFalse();
    }

    @Test
    void 임시_비밀번호로_로그인하면_비밀번호_변경이_필요하다고_알린다() {
        User user = user(1L);
        user.issueTempPassword(passwordEncoder.encode("Temp1234!"));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(EMAIL, "Temp1234!");

        assertThat(response.passwordResetRequired()).isTrue();
        assertThat(jwtTokenProvider.parse(response.accessToken())).contains(new LoginUser(1L, true));
    }

    @Test
    void 이메일은_대소문자를_구분하지_않는다() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L)));

        authService.login("User@Example.COM", RAW_PASSWORD);

        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    void 없는_이메일이면_LOGIN_FAILED() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertLoginFailed(() -> authService.login(EMAIL, RAW_PASSWORD));
    }

    @Test
    void 비밀번호가_틀리면_LOGIN_FAILED() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user(1L)));

        assertLoginFailed(() -> authService.login(EMAIL, "Wrong123!"));
    }

    @Test
    void 탈퇴_회원은_비밀번호가_맞아도_LOGIN_FAILED() {
        User user = user(1L);
        user.withdraw(LocalDateTime.now());
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertLoginFailed(() -> authService.login(EMAIL, RAW_PASSWORD));
    }

    private User user(Long id) {
        User user = User.builder()
                .email(EMAIL)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .name("박기택")
                .nickname("기택")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void assertLoginFailed(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.LOGIN_FAILED));
    }
}
