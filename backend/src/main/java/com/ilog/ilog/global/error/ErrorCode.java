package com.ilog.ilog.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    INVALID_PASSWORD_FORMAT(HttpStatus.BAD_REQUEST, "비밀번호 형식이 올바르지 않습니다."),
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
    PASSWORD_REUSED(HttpStatus.BAD_REQUEST, "최근 사용한 비밀번호는 사용할 수 없습니다."),
    NICKNAME_UNCHANGED(HttpStatus.BAD_REQUEST, "현재 닉네임과 같습니다."),           // 프론트 처리 시 제거
    HASHTAG_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "해시태그 개수를 초과했습니다."),    // D-09 후 사용

    // 401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다."),             // [논의 필요]
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),      // [논의 필요] 400 전환 검토
    TEMP_PASSWORD_EXPIRED(HttpStatus.UNAUTHORIZED, "임시 비밀번호가 만료되었습니다."), // D-12 후 사용

    // 403
    POST_NOT_OWNER(HttpStatus.FORBIDDEN, "작성자만 수정·삭제할 수 있습니다."),
    MEMBER_WITHDRAWN(HttpStatus.FORBIDDEN, "탈퇴한 계정입니다."),                  // [논의 필요]

    // 404
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원 정보를 찾을 수 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."),
    HASHTAG_NOT_FOUND(HttpStatus.NOT_FOUND, "해시태그를 찾을 수 없습니다."),
    API_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 API입니다."),               // [추가 제안]

    // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."), // [추가 제안]

    // 409
    MEMBER_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    MEMBER_DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    REJOIN_RESTRICTED(HttpStatus.CONFLICT, "탈퇴 후 재가입이 제한된 이메일입니다."),

    // 500
    MAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메일 발송에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}
