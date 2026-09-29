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
        user.issueTempPassword(passwordEncoder.encode("Temp1234!"), LocalDateTime.now().plusHours(1));
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
    void 임시_비밀번호가_만료됐으면_TEMP_PASSWORD_EXPIRED() {
        // U6: 발급 후 24시간. 만료되면 다시 발급받아야 한다.
        User user = user(1L);
        user.issueTempPassword(passwordEncoder.encode("Temp1234!"), LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(EMAIL, "Temp1234!"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.TEMP_PASSWORD_EXPIRED));
    }

    /**
     * U2 로 기대값이 뒤집힌 테스트다. 예전에는 탈퇴 회원도 LOGIN_FAILED 였지만,
     * 이제 비밀번호까지 맞은 경우에만 탈퇴 사실을 알려 준다(프론트가 복구 안내를 띄워야 해서).
     */
    @Test
    void 탈퇴_30일_이내면_USER_WITHDRAWN() {
        User user = user(1L);
        user.withdraw(LocalDateTime.now().minusDays(29));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(EMAIL, RAW_PASSWORD))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_WITHDRAWN));
    }

    @Test
    void 탈퇴_30일이_지났으면_LOGIN_FAILED() {
        // 곧 물리 삭제될 계정이라 존재 자체를 알리지 않는다
        User user = user(1L);
        user.withdraw(LocalDateTime.now().minusDays(31));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertLoginFailed(() -> authService.login(EMAIL, RAW_PASSWORD));
    }

    @Test
    void 탈퇴_회원이어도_비밀번호가_틀리면_LOGIN_FAILED() {
        // 순서 고정: 비밀번호 검사(2번)가 탈퇴 검사(3번)보다 먼저다.
        // 이메일만 아는 사람에게 계정이 탈퇴 상태라는 것이 드러나면 안 된다 (U2).
        User user = user(1L);
        user.withdraw(LocalDateTime.now().minusDays(29));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertLoginFailed(() -> authService.login(EMAIL, "Wrong123!"));
    }

    // ---------- 탈퇴 계정 복구 (AUTH-04) ----------

    @Test
    void 탈퇴_30일_이내면_복구되고_이후_로그인할_수_있다() {
        User user = user(1L);
        user.withdraw(LocalDateTime.now().minusDays(29));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.recover("User@Example.com", RAW_PASSWORD);

        assertThat(user.isWithdrawn()).isFalse();
        assertThat(authService.login(EMAIL, RAW_PASSWORD).accessToken()).isNotBlank();
    }

    @Test
    void 탈퇴_30일이_지났으면_복구도_LOGIN_FAILED() {
        User user = user(1L);
        user.withdraw(LocalDateTime.now().minusDays(31));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertLoginFailed(() -> authService.recover(EMAIL, RAW_PASSWORD));
        assertThat(user.isWithdrawn()).isTrue();
    }

    @Test
    void 활성_계정을_복구하면_아무것도_바꾸지_않는다() {
        User user = user(1L);
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        authService.recover(EMAIL, RAW_PASSWORD);

        assertThat(user.isWithdrawn()).isFalse();
    }

    @Test
    void 복구_비밀번호가_틀리면_LOGIN_FAILED_이고_탈퇴_상태가_유지된다() {
        User user = user(1L);
        user.withdraw(LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        assertLoginFailed(() -> authService.recover(EMAIL, "Wrong123!"));
        assertThat(user.isWithdrawn()).isTrue();
    }

    @Test
    void 없는_이메일의_복구는_LOGIN_FAILED() {
        assertLoginFailed(() -> authService.recover("none@example.com", RAW_PASSWORD));
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
