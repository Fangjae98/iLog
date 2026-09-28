package com.ilog.ilog.user.domain;

import java.util.regex.Pattern;

/**
 * 회원 입력값 규칙 (API 명세서 4장, D-16).
 * 회원가입·닉네임 수정·비밀번호 변경이 같은 규칙을 쓰도록 한 곳에 둔다.
 */
public final class UserPolicy {

    public static final String NICKNAME_REGEX = "^[가-힣A-Za-z0-9]{2,10}$";
    public static final String NICKNAME_MESSAGE = "닉네임은 2~10자의 한글, 영문, 숫자만 가능합니다.";

    /**
     * 탈퇴 후 복구 가능 기간 (U1, D-10).
     * 이 기간이 지나면 스케줄러가 물리 삭제하고(U4), 같은 이메일·닉네임을 다시 쓸 수 있다.
     * 로그인(T04)·탈퇴(T10)·복구(T11)·스케줄러(T12)가 같은 값을 봐야 해서 여기에 둔다.
     */
    public static final int WITHDRAWAL_RECOVERY_DAYS = 30;

    /**
     * 재사용을 막을 최근 비밀번호 개수 (U7, D-13). 현재 비밀번호를 포함해 센다.
     * 임시 비밀번호는 이력에 넣지 않는다.
     */
    public static final int PASSWORD_HISTORY_LIMIT = 3;

    /** 비밀번호에 쓸 수 있는 특수문자. 아래 PASSWORD 정규식과 같은 집합이어야 한다. */
    public static final String PASSWORD_SPECIAL_CHARACTERS = "!@#$%^&*";

    /** 임시 비밀번호 길이 (AUTH-03). PASSWORD 정규식의 8~20자 범위 안에 있어야 한다. */
    public static final int TEMP_PASSWORD_LENGTH = 12;

    /** 임시 비밀번호 유효 기간 (U6, D-12). */
    public static final int TEMP_PASSWORD_VALIDITY_HOURS = 24;

    /** 8~20자, 영문·숫자·특수문자(!@#$%^&*) 각 1개 이상. */
    private static final Pattern PASSWORD = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,20}$");

    private UserPolicy() {
    }

    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD.matcher(password).matches();
    }

    /** 문자열 입력은 앞뒤 공백을 제거한 뒤 검증한다 (명세서 1장). 비밀번호에는 쓰지 않는다. */
    public static String trim(String value) {
        return value == null ? null : value.trim();
    }
}
