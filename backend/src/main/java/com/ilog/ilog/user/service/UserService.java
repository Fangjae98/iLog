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

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordHistoryService passwordHistoryService;

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
        User saved;
        try {
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // 사전 검사와 저장 사이에 같은 값이 먼저 들어간 경우. 유니크 제약이 최종 방어선이다.
            validateEmailNotTaken(email);
            validateNicknameNotTaken(request.nickname());
            throw e;
        }

        // 반드시 try 밖에서, 저장 성공 뒤에 부른다 (U7).
        //  - FK 가 회원 행을 요구하므로 insert 뒤여야 한다.
        //  - try 안에 두면 이력 저장 실패가 위 catch 로 들어가, 중복이 아닌데
        //    중복 이메일/닉네임이라고 잘못 보고하게 된다.
        // 가입과 이력을 묶는 트랜잭션은 없다. 그 사이에 죽으면 이력 행이 비는데,
        // isReused 가 현재 해시도 함께 보므로 현재 비밀번호 재사용 구멍은 생기지 않는다.
        passwordHistoryService.record(saved.getId(), saved.getPassword());
        return saved.getId();
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

    /**
     * 비밀번호 변경 (MBR-07).
     *
     * <p>아래 순서가 곧 정책이다 (지시서 T06).
     * <ol>
     *   <li>현재 비밀번호 불일치 → {@code PASSWORD_MISMATCH}. <b>400 이다</b>.
     *       401 로 하면 프론트가 로그인 만료로 보고 변경 도중에 로그아웃시킨다</li>
     *   <li>새 비밀번호 형식 위반 → {@code INVALID_PASSWORD_FORMAT}</li>
     *   <li>최근 3개(현재 포함) 재사용 → {@code PASSWORD_REUSED}</li>
     *   <li>해시 교체. {@code changePassword} 가 임시 비밀번호 상태도 함께 푼다</li>
     *   <li>이력 저장</li>
     * </ol>
     *
     * <p>임시 비밀번호로 로그인한 상태에서도 이 API 를 쓴다. 그때는 현재 비밀번호 = 임시 비밀번호다.
     * 성공해도 토큰은 그대로 쓴다 (A7). 기존 토큰의 {@code tmp=true} 는 만료까지 남지만
     * 서버는 그것으로 아무것도 막지 않는다 (A8).
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = getActiveUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        if (!UserPolicy.isValidPassword(newPassword)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_FORMAT);
        }
        if (passwordHistoryService.isReused(userId, user.getPassword(), newPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_REUSED);
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        passwordHistoryService.record(userId, user.getPassword());
    }

    /**
     * 임시 비밀번호로 교체한다 (AUTH-03).
     *
     * <p>메일 발송이 끝난 뒤에만 불러야 한다. 발송 전에 부르면 사용자가 받지 못한
     * 비밀번호로 계정이 잠긴다. 호출 순서는 {@code TemporaryPasswordService} 가 지킨다.
     *
     * <p>임시 비밀번호는 {@code password_history} 에 넣지 않는다 (U7).
     */
    @Transactional
    public void issueTempPassword(Long userId, String encodedTempPassword, LocalDateTime expiresAt) {
        getActiveUser(userId).issueTempPassword(encodedTempPassword, expiresAt);
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
