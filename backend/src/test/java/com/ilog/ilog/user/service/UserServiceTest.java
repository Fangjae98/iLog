package com.ilog.ilog.user.service;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.dto.EmailAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameUpdateResponse;
import com.ilog.ilog.user.dto.PasswordVerificationResponse;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.dto.WithdrawalResponse;
import com.ilog.ilog.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String RAW_PASSWORD = "Passw0rd!";

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordHistoryService passwordHistoryService;

    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    UserService userService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder, passwordHistoryService);
    }

    // ---------- 회원가입 ----------

    @Test
    void 가입하면_이메일은_소문자로_비밀번호는_해시로_저장하고_ID를_돌려준다() {
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        Long id = userService.signup(new SignupRequest("User@Example.com", RAW_PASSWORD, "박기택", "기택"));

        assertThat(id).isEqualTo(7L);
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getName()).isEqualTo("박기택");
        assertThat(saved.getNickname()).isEqualTo("기택");
        assertThat(saved.getPassword()).isNotEqualTo(RAW_PASSWORD);
        assertThat(passwordEncoder.matches(RAW_PASSWORD, saved.getPassword())).isTrue();
        assertThat(saved.isTempPassword()).isFalse();
        assertThat(saved.isWithdrawn()).isFalse();
    }

    @Test
    void 가입에_성공하면_비밀번호_이력을_1건_남긴다() {
        // U7: 가입 직후부터 재사용 검사가 동작해야 한다
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 7L);
            return saved;
        });

        userService.signup(new SignupRequest("user@example.com", RAW_PASSWORD, "박기택", "기택"));

        ArgumentCaptor<String> hash = ArgumentCaptor.forClass(String.class);
        verify(passwordHistoryService).record(eq(7L), hash.capture());
        assertThat(passwordEncoder.matches(RAW_PASSWORD, hash.getValue())).isTrue();
        assertThat(hash.getValue()).isNotEqualTo(RAW_PASSWORD);   // 평문이 넘어가면 안 된다
    }

    @Test
    void 가입에_실패하면_비밀번호_이력을_남기지_않는다() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(activeUser(1L, "a@b.com", "다른닉")));

        assertBusiness(() -> userService.signup(new SignupRequest("a@b.com", RAW_PASSWORD, "박기택", "기택")),
                ErrorCode.USER_DUPLICATE_EMAIL);
        verifyNoInteractions(passwordHistoryService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Pa0!",                      // 8자 미만
            "Passw0rd!Passw0rd!Pass1",   // 20자 초과
            "Password!",                 // 숫자 없음
            "12345678!",                 // 영문 없음
            "Passw0rdd",                 // 특수문자 없음
            "Passw0rd!?",                // 허용되지 않은 특수문자
            "Pass w0rd!"                 // 공백
    })
    void 비밀번호_형식이_틀리면_INVALID_PASSWORD_FORMAT(String password) {
        assertBusiness(() -> userService.signup(new SignupRequest("a@b.com", password, "박기택", "기택")),
                ErrorCode.INVALID_PASSWORD_FORMAT);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_가입된_이메일이면_USER_DUPLICATE_EMAIL() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(activeUser(1L, "a@b.com", "다른닉")));

        assertBusiness(() -> userService.signup(new SignupRequest("A@b.com", RAW_PASSWORD, "박기택", "기택")),
                ErrorCode.USER_DUPLICATE_EMAIL);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 탈퇴한_계정의_이메일이면_REJOIN_RESTRICTED() {
        User withdrawn = activeUser(1L, "a@b.com", "탈퇴닉");
        withdrawn.withdraw(LocalDateTime.now().minusDays(3));
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(withdrawn));

        assertBusiness(() -> userService.signup(new SignupRequest("a@b.com", RAW_PASSWORD, "박기택", "기택")),
                ErrorCode.REJOIN_RESTRICTED);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 이미_쓰는_닉네임이면_USER_DUPLICATE_NICKNAME() {
        when(userRepository.existsByNickname("기택")).thenReturn(true);

        assertBusiness(() -> userService.signup(new SignupRequest("a@b.com", RAW_PASSWORD, "박기택", "기택")),
                ErrorCode.USER_DUPLICATE_NICKNAME);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void 사전검사_통과_후_동시가입으로_이메일이_겹치면_USER_DUPLICATE_EMAIL() {
        when(userRepository.findByEmail("a@b.com"))
                .thenReturn(Optional.empty())                                   // 사전 검사
                .thenReturn(Optional.of(activeUser(1L, "a@b.com", "먼저가입")));  // 저장 실패 후 재확인
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk_users_email"));

        assertBusiness(() -> userService.signup(new SignupRequest("a@b.com", RAW_PASSWORD, "박기택", "기택")),
                ErrorCode.USER_DUPLICATE_EMAIL);
    }

    @Test
    void 사전검사_통과_후_동시가입으로_닉네임이_겹치면_USER_DUPLICATE_NICKNAME() {
        when(userRepository.existsByNickname("기택")).thenReturn(false).thenReturn(true);
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("uk_users_nickname"));

        assertBusiness(() -> userService.signup(new SignupRequest("a@b.com", RAW_PASSWORD, "박기택", "기택")),
                ErrorCode.USER_DUPLICATE_NICKNAME);
    }

    @Test
    void 저장_실패의_원인이_이메일도_닉네임도_아니면_원래_예외를_그대로_던진다() {
        DataIntegrityViolationException cause = new DataIntegrityViolationException("something else");
        when(userRepository.saveAndFlush(any(User.class))).thenThrow(cause);

        assertThatThrownBy(() -> userService.signup(new SignupRequest("a@b.com", RAW_PASSWORD, "박기택", "기택")))
                .isSameAs(cause);
    }

    // ---------- 중복 확인 ----------

    @Test
    void 이메일이_없으면_사용_가능() {
        EmailAvailabilityResponse response = userService.checkEmail("a@b.com");

        assertThat(response.available()).isTrue();
        assertThat(response.reason()).isNull();
    }

    @Test
    void 이메일이_사용중이면_DUPLICATE() {
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(activeUser(1L, "a@b.com", "기택")));

        EmailAvailabilityResponse response = userService.checkEmail("A@B.com");   // 대소문자 무관

        assertThat(response.available()).isFalse();
        assertThat(response.reason()).isEqualTo(EmailAvailabilityResponse.Reason.DUPLICATE);
    }

    @Test
    void 이메일이_탈퇴_계정이면_WITHDRAWN() {
        User withdrawn = activeUser(1L, "a@b.com", "기택");
        withdrawn.withdraw(LocalDateTime.now());
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(withdrawn));

        EmailAvailabilityResponse response = userService.checkEmail("a@b.com");

        assertThat(response.available()).isFalse();
        assertThat(response.reason()).isEqualTo(EmailAvailabilityResponse.Reason.WITHDRAWN);
    }

    @Test
    void 닉네임_사용_가능_여부() {
        when(userRepository.existsByNickname("사용중")).thenReturn(true);

        assertThat(userService.checkNickname("사용중")).isEqualTo(new NicknameAvailabilityResponse(false));
        assertThat(userService.checkNickname("새닉네임")).isEqualTo(new NicknameAvailabilityResponse(true));
    }

    // ---------- 비밀번호 재확인 ----------

    @Test
    void 비밀번호가_맞으면_개인정보를_돌려준다() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        PasswordVerificationResponse response = userService.verifyPassword(1L, RAW_PASSWORD);

        assertThat(response.email()).isEqualTo("a@b.com");
        assertThat(response.name()).isEqualTo("박기택");
        assertThat(response.nickname()).isEqualTo("기택");
    }

    @Test
    void 비밀번호가_틀리면_PASSWORD_MISMATCH() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L, "a@b.com", "기택")));

        assertBusiness(() -> userService.verifyPassword(1L, "Wrong0!pw"), ErrorCode.PASSWORD_MISMATCH);
    }

    @Test
    void 재확인_대상_회원이_없으면_USER_NOT_FOUND() {
        assertBusiness(() -> userService.verifyPassword(99L, RAW_PASSWORD), ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void 탈퇴한_회원의_재확인은_UNAUTHORIZED() {
        User withdrawn = activeUser(1L, "a@b.com", "기택");
        withdrawn.withdraw(LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(withdrawn));

        assertBusiness(() -> userService.verifyPassword(1L, RAW_PASSWORD), ErrorCode.UNAUTHORIZED);
    }

    // ---------- 닉네임 수정 ----------

    @Test
    void 닉네임을_바꾼다() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        NicknameUpdateResponse response = userService.updateNickname(1L, "새닉네임");

        assertThat(response).isEqualTo(new NicknameUpdateResponse(1L, "새닉네임"));
        assertThat(user.getNickname()).isEqualTo("새닉네임");
        verify(userRepository).flush();
    }

    @Test
    void 현재_닉네임과_같으면_NICKNAME_UNCHANGED_이고_중복검사보다_먼저다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L, "a@b.com", "기택")));

        assertBusiness(() -> userService.updateNickname(1L, "기택"), ErrorCode.NICKNAME_UNCHANGED);
        verify(userRepository, never()).existsByNickname(any());
    }

    @Test
    void 남이_쓰는_닉네임이면_USER_DUPLICATE_NICKNAME() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("남의닉")).thenReturn(true);

        assertBusiness(() -> userService.updateNickname(1L, "남의닉"), ErrorCode.USER_DUPLICATE_NICKNAME);
        assertThat(user.getNickname()).isEqualTo("기택");
    }

    @Test
    void 사전검사_후_동시에_같은_닉네임이_들어오면_USER_DUPLICATE_NICKNAME() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser(1L, "a@b.com", "기택")));
        doThrow(new DataIntegrityViolationException("uk_users_nickname")).when(userRepository).flush();

        assertBusiness(() -> userService.updateNickname(1L, "새닉네임"), ErrorCode.USER_DUPLICATE_NICKNAME);
    }

    @Test
    void 탈퇴한_회원은_닉네임을_바꿀_수_없다() {
        User withdrawn = activeUser(1L, "a@b.com", "기택");
        withdrawn.withdraw(LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(withdrawn));

        assertBusiness(() -> userService.updateNickname(1L, "새닉네임"), ErrorCode.UNAUTHORIZED);
    }

    // ---------- helpers ----------

    // ---------- 비밀번호 변경 (MBR-07) ----------

    @Test
    void 비밀번호를_바꾸면_해시가_교체되고_이력을_남긴다() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.changePassword(1L, RAW_PASSWORD, "NewPassw0rd!");

        assertThat(passwordEncoder.matches("NewPassw0rd!", user.getPassword())).isTrue();
        assertThat(user.isTempPassword()).isFalse();
        verify(passwordHistoryService).record(1L, user.getPassword());
    }

    @Test
    void 현재_비밀번호가_틀리면_PASSWORD_MISMATCH_이고_400이다() {
        // 401 이면 프론트가 로그인 만료로 보고 변경 도중에 로그아웃시킨다
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertBusiness(() -> userService.changePassword(1L, "Wrong123!", "NewPassw0rd!"),
                ErrorCode.PASSWORD_MISMATCH);
        assertThat(ErrorCode.PASSWORD_MISMATCH.getStatus().value()).isEqualTo(400);
        verifyNoInteractions(passwordHistoryService);
    }

    @Test
    void 새_비밀번호_형식이_틀리면_INVALID_PASSWORD_FORMAT() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertBusiness(() -> userService.changePassword(1L, RAW_PASSWORD, "weak"),
                ErrorCode.INVALID_PASSWORD_FORMAT);
    }

    @Test
    void 최근_비밀번호를_다시_쓰면_PASSWORD_REUSED() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordHistoryService.isReused(eq(1L), anyString(), eq("NewPassw0rd!"))).thenReturn(true);

        assertBusiness(() -> userService.changePassword(1L, RAW_PASSWORD, "NewPassw0rd!"),
                ErrorCode.PASSWORD_REUSED);
        verify(passwordHistoryService, never()).record(any(), any());
    }

    @Test
    void 현재_비밀번호_검사가_새_비밀번호_형식_검사보다_먼저다() {
        // 둘 다 틀렸을 때 어느 쪽을 알려 주는지가 정책이다 (지시서 T06 처리 순서)
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertBusiness(() -> userService.changePassword(1L, "Wrong123!", "weak"),
                ErrorCode.PASSWORD_MISMATCH);
    }

    @Test
    void 탈퇴_회원은_비밀번호를_바꿀_수_없다() {
        User user = activeUser(1L, "a@b.com", "기택");
        user.withdraw(java.time.LocalDateTime.now());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertBusiness(() -> userService.changePassword(1L, RAW_PASSWORD, "NewPassw0rd!"),
                ErrorCode.UNAUTHORIZED);
    }

    // ---------- 회원 탈퇴 (MBR-08) ----------

    @Test
    void 탈퇴하면_탈퇴_시각이_채워지고_30일_뒤를_복구_기한으로_돌려준다() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        WithdrawalResponse response = userService.withdraw(1L, RAW_PASSWORD);

        assertThat(user.isWithdrawn()).isTrue();
        assertThat(response.recoverableUntil()).isEqualTo(user.getWithdrawnAt().plusDays(30));
    }

    @Test
    void 탈퇴_비밀번호가_틀리면_PASSWORD_MISMATCH_이고_탈퇴하지_않는다() {
        User user = activeUser(1L, "a@b.com", "기택");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertBusiness(() -> userService.withdraw(1L, "Wrong0!pw"), ErrorCode.PASSWORD_MISMATCH);
        assertThat(user.isWithdrawn()).isFalse();
    }

    @Test
    void 이미_탈퇴한_회원은_다시_탈퇴할_수_없다() {
        User withdrawn = activeUser(1L, "a@b.com", "기택");
        withdrawn.withdraw(LocalDateTime.now().minusDays(1));
        when(userRepository.findById(1L)).thenReturn(Optional.of(withdrawn));

        assertBusiness(() -> userService.withdraw(1L, RAW_PASSWORD), ErrorCode.UNAUTHORIZED);
    }

    private User activeUser(Long id, String email, String nickname) {
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(RAW_PASSWORD))
                .name("박기택")
                .nickname(nickname)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private void assertBusiness(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
}
