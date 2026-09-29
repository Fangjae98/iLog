package com.ilog.ilog.post.domain;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 게시글 입력값 규칙 (P1~P4, D-08·D-09).
 * 등록·수정·검색이 같은 규칙을 쓰도록 한 곳에 둔다. ({@code UserPolicy} 와 같은 방식)
 */
public final class PostPolicy {

    /** 제목 최대 길이. 엔티티 컬럼 길이(VARCHAR(100))와 같아야 한다. */
    public static final int TITLE_MAX = 100;

    /** 본문 최대 길이 (9/28 결정). 컬럼은 TEXT 라 길이는 DTO 에서만 검사한다. */
    public static final int CONTENT_MAX = 1000;

    /** 글 하나의 URL 최대 개수 (D-08). URL 은 선택이라 0개도 된다. */
    public static final int URL_MAX_COUNT = 5;

    /** URL 한 개의 최대 길이 (D-08). */
    public static final int URL_MAX_LENGTH = 2048;

    /** {@code http://} 또는 {@code https://} 로 시작하고 공백이 없어야 한다. */
    public static final String URL_REGEX = "^https?://\\S+$";

    /** 글 하나의 해시태그 최대 개수 (D-09). 다듬은 뒤의 개수로 센다. */
    public static final int HASHTAG_MAX_COUNT = 10;

    /** 다듬은 해시태그의 최대 길이. 엔티티 컬럼 길이(VARCHAR(20))와 같아야 한다. */
    public static final int HASHTAG_MAX_LENGTH = 20;

    /** 다듬은 해시태그: 1~20자, 한글·영문 소문자·숫자·{@code _} 만 (D-09). */
    private static final Pattern HASHTAG = Pattern.compile("^[가-힣a-z0-9_]{1," + HASHTAG_MAX_LENGTH + "}$");

    private PostPolicy() {
    }

    /**
     * 해시태그 하나를 다듬는다 (P3). 저장과 검색이 같은 값을 보도록 둘 다 이 메서드를 쓴다.
     *
     * <p>앞뒤 공백 제거 → 맨 앞 {@code #} 제거 → 영문 소문자.
     * 예: {@code " #Spring "} → {@code "spring"}, {@code "#"} → {@code ""}
     */
    public static String normalizeHashtag(String raw) {
        return raw.trim()
                .replaceAll("^#+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 목록 전체를 다듬고 중복을 없앤다. 소문자로 바꾼 뒤에 중복을 없애야 {@code Spring} 과 {@code spring} 이 하나가 된다.
     * 빈 값은 남겨 둔다. 저장은 빈 값을 형식 오류로, 검색은 무시하는 식으로 쓰는 쪽이 정한다.
     */
    public static List<String> normalizeHashtags(List<String> raws) {
        return raws.stream()
                .map(PostPolicy::normalizeHashtag)
                .distinct()
                .toList();
    }

    public static boolean isValidHashtag(String normalized) {
        return HASHTAG.matcher(normalized).matches();
    }

    /** 목록의 문자열을 하나씩 trim 한다. null 목록·null 항목은 그대로 둔다(검증 애노테이션이 잡는다). */
    public static List<String> trimAll(List<String> values) {
        if (values == null) {
            return null;
        }
        return values.stream()
                .map(value -> value == null ? null : value.trim())
                .toList();
    }
}
