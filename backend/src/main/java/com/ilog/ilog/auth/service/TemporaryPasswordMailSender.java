package com.ilog.ilog.auth.service;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.user.domain.UserPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 임시 비밀번호 안내 메일 (AUTH-03, U9: Gmail SMTP).
 *
 * <p>발송 실패는 {@code MAIL_SEND_FAILED}(500) 로 올린다. 호출부는 이 예외가 나면
 * 비밀번호를 바꾸지 않고 그대로 둔다 — 못 받은 비밀번호로 계정이 잠기면 안 되기 때문이다.
 *
 * <p>임시 비밀번호는 <b>로그에 남기지 않는다.</b>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemporaryPasswordMailSender {

    private static final String SUBJECT = "[일일로그] 임시 비밀번호 안내";

    private final JavaMailSender mailSender;

    public void send(String to, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(SUBJECT);
        message.setText(body(temporaryPassword));

        try {
            mailSender.send(message);
        } catch (MailException e) {
            // 수신자 주소는 개인정보라 로그에 남기지 않는다
            log.error("임시 비밀번호 메일 발송에 실패했습니다.", e);
            throw new BusinessException(ErrorCode.MAIL_SEND_FAILED);
        }
    }

    private String body(String temporaryPassword) {
        return """
                안녕하세요, 일일로그입니다.

                요청하신 임시 비밀번호는 아래와 같습니다.

                    %s

                이 임시 비밀번호는 발급 후 %d시간 동안만 쓸 수 있습니다.
                로그인한 뒤 마이페이지에서 새 비밀번호로 바꿔 주세요.

                본인이 요청한 것이 아니라면 이 메일을 무시하셔도 됩니다.
                기존 비밀번호는 이미 임시 비밀번호로 바뀌었으니, 비밀번호 찾기를 다시 진행해 주세요.
                """.formatted(temporaryPassword, UserPolicy.TEMP_PASSWORD_VALIDITY_HOURS);
    }
}
