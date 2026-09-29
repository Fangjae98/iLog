package com.ilog.ilog.post.dto;

import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/*
 * [게시글 목록·검색 조건] GET /api/v1/posts 의 쿼리 파라미터 (S1~S5)
 *
 *   GET /api/v1/posts                                  ← 전체 목록 첫 쪽
 *   GET /api/v1/posts?keyword=jpa&hashtag=spring       ← 제목·내용에 jpa가 있고 spring 태그가 붙은 글
 *   GET /api/v1/posts?author=me&page=1                 ← 내 글 두 번째 쪽
 *
 * 모든 조건은 선택이고, 여러 개를 보내면 모두 만족하는 글만 찾는다(AND).
 * 여기 없는 파라미터(프론트가 보내는 sort 등)는 무시한다. 정렬은 최신순 고정이다(S4).
 *
 * 쿼리 파라미터를 record로 받으려면 컨트롤러에서 @ModelAttribute를 쓴다.
 * 형식이 틀린 값(date=어제, page=abc)은 검증 실패와 같게 400 INVALID_INPUT이 된다.
 */
public record PostSearchRequest(

        @Schema(description = "제목 **또는** 본문에 이 글자가 들어간 글. 대소문자를 구분하지 않는다.", example = "jpa")
        String keyword,

        @Schema(description = "제목에 이 글자가 들어간 글. 대소문자를 구분하지 않는다.", example = "스프링")
        String title,

        @Schema(description = "작성자 닉네임이 정확히 같은 글. 프론트의 '내 글' 버튼은 내 닉네임으로 이 값을 보낸다.", example = "기택")
        String nickname,

        @Schema(description = "이 해시태그가 **모두** 붙은 글. 여러 개는 `hashtag=spring&hashtag=jpa` 처럼 반복해서 보낸다. "
                + "저장할 때와 같이 `#` 과 공백을 지우고 소문자로 바꿔 비교한다. 다듬은 뒤 10개를 넘으면 400 `INVALID_INPUT`.",
                example = "[\"spring\"]")
        List<String> hashtag,

        @Schema(description = "이 날짜(KST)에 작성된 글. `yyyy-MM-dd`", example = "2026-09-16")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate date,

        @Schema(description = "`me` 면 로그인한 회원이 쓴 글만. 다른 값이면 400 `INVALID_INPUT`.", example = "me")
        @Pattern(regexp = "me", message = "author 는 me 만 쓸 수 있습니다.")
        String author,

        @Schema(description = "쪽 번호. 0부터 시작한다.", defaultValue = "0", example = "0")
        @PositiveOrZero(message = "쪽 번호는 0 이상이어야 합니다.")
        Integer page,

        @Schema(description = "한 쪽에 담을 글 수. 1~50.", defaultValue = "10", example = "10")
        @Min(value = 1, message = "쪽 크기는 1~50 이어야 합니다.")
        @Max(value = 50, message = "쪽 크기는 1~50 이어야 합니다.")
        Integer size
) {

    /** 쪽 크기 기본값 (S4). */
    public static final int DEFAULT_SIZE = 10;

    // 빈 검색어("", "  ")는 조건이 없는 것으로 본다. 안 보낸 page·size는 기본값으로 채운다.
    public PostSearchRequest {
        keyword = blankToNull(keyword);
        title = blankToNull(title);
        nickname = blankToNull(nickname);
        hashtag = hashtag == null ? List.of() : hashtag;
        page = page == null ? 0 : page;
        size = size == null ? DEFAULT_SIZE : size;
    }

    public boolean onlyMine() {
        return "me".equals(author);
    }

    private static String blankToNull(String value) {
        String trimmed = UserPolicy.trim(value);
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }
}
