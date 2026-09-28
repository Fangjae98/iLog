package com.ilog.ilog.auth.service;

import com.ilog.ilog.auth.dto.TemporaryPasswordResponse;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.domain.UserPolicy;
import com.ilog.ilog.user.repository.UserRepository;
import com.ilog.ilog.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * 임시 비밀번호 발급 (AUTH-03).
 *
 * <p><b>메일을 먼저 보내고, 성공한 뒤에 비밀번호를 바꾼다.</b> 순서를 뒤집으면
 * 발송에 실패했을 때 사용자가 받지 못한 비밀번호로 계정이 잠긴다.
 *
 * <p>그래서 이 클래스에는 {@code @Transactional} 을 걸지 않는다. 메일 발송 단계는
 * 아무것도 쓰지 않은 채로 실패할 수 있어야 한다. 저장은 {@link UserService} 를 통해
 * <b>다른 빈</b>으로 호출해야 프록시가 트랜잭션을 연다.
 */
@Service
@RequiredArgsConstructor
public class TemporaryPasswordService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final TemporaryPasswordGenerator generator;
    private final TemporaryPasswordMailSender mailSender;
    private final PasswordEncoder passwordEncoder;

    public TemporaryPasswordResponse issue(String email, String name) {
        // 탈퇴 회원도 404 로 응답해 계정 상태를 드러내지 않는다 (U8)
        User user = userRepository
                .findByEmailAndNameAndWithdrawnAtIsNull(normalizeEmail(email), name)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String temporaryPassword = generator.generate();

        // 실패하면 MAIL_SEND_FAILED 가 올라가고 기존 비밀번호는 그대로 남는다
        mailSender.send(user.getEmail(), temporaryPassword);

        userService.issueTempPassword(
                user.getId(),
                passwordEncoder.encode(temporaryPassword),
                LocalDateTime.now().plusHours(UserPolicy.TEMP_PASSWORD_VALIDITY_HOURS));

        // 임시 비밀번호는 password_history 에 넣지 않는다 (U7). 누락이 아니라 의도다.
        return TemporaryPasswordResponse.of(user.getEmail());
    }

    /** UserService.normalizeEmail 과 같은 규칙 (D-16). */
    private String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
