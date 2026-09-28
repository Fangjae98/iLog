package com.ilog.ilog.auth.service;

import com.ilog.ilog.auth.dto.TemporaryPasswordResponse;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.domain.UserPolicy;
import com.ilog.ilog.user.repository.UserRepository;
import com.ilog.ilog.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 임시 비밀번호 발급 (AUTH-03, T07 완료 기준). */
@ExtendWith(MockitoExtension.class)
class TemporaryPasswordServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String NAME = "박기택";

    @Mock
    UserRepository userRepository;

    @Mock
    UserService userService;

    @Mock
    TemporaryPasswordMailSender mailSender;

    // 진짜 구현을 쓴다. 생성기는 규칙을 만족하는 값을 내야 하고, 인코더는 해시 비교가 필요하다.
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    TemporaryPasswordService temporaryPasswordService;

    @BeforeEach
    void setUp() {
        temporaryPasswordService = new TemporaryPasswordService(
                userRepository, userService, new TemporaryPasswordGenerator(), mailSender, passwordEncoder);
    }

    @Test
    void 발급하면_메일을_보내고_비밀번호를_임시로_바꾼다() {
        // 스터빙 밖에서 미리 만든다. when(...) 안에서 인코더를 부르면 Mockito 스터빙이 꼬인다.
        User found = user(1L);
        when(userRepository.findByEmailAndNameAndWithdrawnAtIsNull(EMAIL, NAME))
                .thenReturn(Optional.of(found));

        TemporaryPasswordResponse response = temporaryPasswordService.issue(EMAIL, NAME);

        // 메일로 보낸 것과 저장한 해시가 같은 비밀번호여야 한다
        ArgumentCaptor<String> sent = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(eqEmail(), sent.capture());

        ArgumentCaptor<String> storedHash = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiresAt = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(userService).issueTempPassword(eq(1L),
                storedHash.capture(), expiresAt.capture());

        assertThat(passwordEncoder.matches(sent.getValue(), storedHash.getValue())).isTrue();
        assertThat(storedHash.getValue()).isNotEqualTo(sent.getValue());   // 평문 저장 금지
        LocalDateTime expected = LocalDateTime.now().plusHours(UserPolicy.TEMP_PASSWORD_VALIDITY_HOURS);
        assertThat(expiresAt.getValue())
                .isAfter(expected.minusMinutes(1))
                .isBefore(expected.plusMinutes(1));
        assertThat(response.email()).isEqualTo("u***@example.com");
    }

    @Test
    void 메일_발송에_실패하면_기존_비밀번호를_그대로_둔다() {
        // 완료 기준: 못 받은 비밀번호로 계정이 잠기면 안 된다
        User found = user(1L);
        when(userRepository.findByEmailAndNameAndWithdrawnAtIsNull(EMAIL, NAME))
                .thenReturn(Optional.of(found));
        doThrow(new BusinessException(ErrorCode.MAIL_SEND_FAILED))
                .when(mailSender).send(anyString(), anyString());

        assertThatThrownBy(() -> temporaryPasswordService.issue(EMAIL, NAME))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MAIL_SEND_FAILED));

        verify(userService, never()).issueTempPassword(anyLong(), anyString(), any());
    }

    @Test
    void 이메일이나_이름이_맞지_않으면_USER_NOT_FOUND() {
        when(userRepository.findByEmailAndNameAndWithdrawnAtIsNull(anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> temporaryPasswordService.issue(EMAIL, "다른이름"))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND));

        verify(mailSender, never()).send(anyString(), anyString());
    }

    @Test
    void 이메일은_소문자로_바꿔_조회한다() {
        User found = user(1L);
        when(userRepository.findByEmailAndNameAndWithdrawnAtIsNull(EMAIL, NAME))
                .thenReturn(Optional.of(found));

        temporaryPasswordService.issue("User@Example.COM", NAME);

        verify(userRepository).findByEmailAndNameAndWithdrawnAtIsNull(EMAIL, NAME);
    }

    private User user(Long id) {
        User user = User.builder()
                .email(EMAIL)
                .password(passwordEncoder.encode("Passw0rd!"))
                .name(NAME)
                .nickname("기택")
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private String eqEmail() {
        return eq(EMAIL);
    }
}
