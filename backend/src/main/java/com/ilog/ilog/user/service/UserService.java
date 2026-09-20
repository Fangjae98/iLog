package com.ilog.ilog.user.service;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.domain.UserPolicy;
import com.ilog.ilog.user.dto.EmailAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameAvailabilityResponse;
import com.ilog.ilog.user.dto.NicknameUpdateResponse;
import com.ilog.ilog.user.dto.PasswordVerificationResponse;
import com.ilog.ilog.user.dto.SignupRequest;
import com.ilog.ilog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 (MBR-01).
     *
     * 트랜잭션을 걸지 않는다. 동시 가입으로 유니크 제약에 걸리면 flush 시점에 예외가 나는데,
     * PostgreSQL 은 실패한 트랜잭션 안에서 추가 조회를 할 수 없어서 원인(이메일/닉네임)을
     * 다시 확인하려면 저장 트랜잭션이 끝난 뒤여야 한다.
     */
    public Long signup(SignupRequest request) {
        if (!UserPolicy.isValidPassword(request.password())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        String email = normalizeEmail(request.email());

        validateEmailNotTaken(email);
        validateNicknameNotTaken(request.nickname());

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .nickname(request.nickname())
                .build();
        try {
            return userRepository.saveAndFlush(user).getId();
        } catch (DataIntegrityViolationException e) {
            // 사전 검사와 저장 사이에 같은 값이 먼저 들어간 경우. 유니크 제약이 최종 방어선이다.
            validateEmailNotTaken(email);
            validateNicknameNotTaken(request.nickname());
            throw e;
        }
    }

    /** 이메일 사용 가능 확인 (MBR-02). 탈퇴 후 30일 이내 계정은 WITHDRAWN 으로 구분한다. */
    @Transactional(readOnly = true)
    public EmailAvailabilityResponse checkEmail(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(user -> EmailAvailabilityResponse.ofUnavailable(user.isWithdrawn()
                        ? EmailAvailabilityResponse.Reason.WITHDRAWN
                        : EmailAvailabilityResponse.Reason.DUPLICATE))
                .orElseGet(EmailAvailabilityResponse::ofAvailable);
    }

    /** 닉네임 사용 가능 확인 (MBR-03). 탈퇴 후 30일 이내 회원의 닉네임도 사용 중으로 본다. */
    @Transactional(readOnly = true)
    public NicknameAvailabilityResponse checkNickname(String nickname) {
        return new NicknameAvailabilityResponse(!userRepository.existsByNickname(nickname));
    }

    /** 비밀번호 재확인 + 개인정보 조회 (MBR-05). */
    @Transactional(readOnly = true)
    public PasswordVerificationResponse verifyPassword(Long userId, String password) {
        User user = getActiveUser(userId);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        return PasswordVerificationResponse.from(user);
    }

    /** 닉네임 수정 (MBR-06). */
    @Transactional
    public NicknameUpdateResponse updateNickname(Long userId, String nickname) {
        User user = getActiveUser(userId);
        if (user.getNickname().equals(nickname)) {
            throw new BusinessException(ErrorCode.NICKNAME_UNCHANGED);
        }
        validateNicknameNotTaken(nickname);

        user.changeNickname(nickname);
        try {
            userRepository.flush();
        } catch (DataIntegrityViolationException e) {   // 사전 검사 이후 다른 회원이 먼저 쓴 경우
            throw new BusinessException(ErrorCode.USER_DUPLICATE_NICKNAME);
        }
        return new NicknameUpdateResponse(user.getId(), user.getNickname());
    }

    /**
     * 탈퇴하지 않은 회원을 조회한다.
     * 탈퇴 회원의 토큰은 인증 필터에서 401 로 막히지만(명세서 2장), 필터가 붙기 전에도
     * 같은 결과가 되도록 서비스에서도 막는다.
     */
    private User getActiveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (user.isWithdrawn()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 30일이 지난 탈퇴 회원은 스케줄러(3단계)가 물리 삭제하므로,
     * 행이 남아 있는 탈퇴 회원은 모두 재가입 제한 기간 안에 있다.
     */
    private void validateEmailNotTaken(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            throw new BusinessException(user.isWithdrawn()
                    ? ErrorCode.REJOIN_RESTRICTED
                    : ErrorCode.USER_DUPLICATE_EMAIL);
        });
    }

    private void validateNicknameNotTaken(String nickname) {
        if (userRepository.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.USER_DUPLICATE_NICKNAME);
        }
    }

    /** 이메일은 대소문자 구분 없이 저장한다 (D-16). */
    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
