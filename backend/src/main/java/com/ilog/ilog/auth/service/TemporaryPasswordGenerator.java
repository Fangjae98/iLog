package com.ilog.ilog.auth.service;

import com.ilog.ilog.user.domain.UserPolicy;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 임시 비밀번호 생성기 (AUTH-03).
 *
 * <p>{@link UserPolicy#isValidPassword} 를 반드시 통과해야 한다. 그래야 받은 사람이
 * 그 비밀번호로 바로 로그인할 수 있다. 무작위로 뽑고 검사해서 다시 뽑는 방식 대신,
 * 각 종류를 1개씩 먼저 배치한 뒤 섞어서 한 번에 조건을 만족시킨다.
 */
@Component
public class TemporaryPasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";   // I, O 제외 (1, 0 과 헷갈림)
    private static final String LOWERCASE = "abcdefghijkmnpqrstuvwxyz";   // l, o 제외
    private static final String DIGITS = "23456789";                      // 0, 1 제외
    private static final String SPECIALS = UserPolicy.PASSWORD_SPECIAL_CHARACTERS;
    private static final String ALL = UPPERCASE + LOWERCASE + DIGITS + SPECIALS;

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        List<Character> chars = new ArrayList<>(UserPolicy.TEMP_PASSWORD_LENGTH);
        // 정규식이 요구하는 세 종류를 먼저 1개씩 확보한다
        chars.add(pick(UPPERCASE + LOWERCASE));
        chars.add(pick(DIGITS));
        chars.add(pick(SPECIALS));
        while (chars.size() < UserPolicy.TEMP_PASSWORD_LENGTH) {
            chars.add(pick(ALL));
        }
        Collections.shuffle(chars, random);

        StringBuilder sb = new StringBuilder(chars.size());
        chars.forEach(sb::append);
        String generated = sb.toString();

        // 방어적 확인. 문자 집합과 정규식이 어긋나면 메일을 보내기 전에 여기서 터진다.
        if (!UserPolicy.isValidPassword(generated)) {
            throw new IllegalStateException("생성한 임시 비밀번호가 비밀번호 규칙을 만족하지 않습니다.");
        }
        return generated;
    }

    private char pick(String candidates) {
        return candidates.charAt(random.nextInt(candidates.length()));
    }
}
