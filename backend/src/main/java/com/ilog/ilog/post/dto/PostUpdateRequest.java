package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.domain.PostPolicy;
import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/*
 * [게시글 수정 요청 DTO]
 *
 *   PATCH /api/v1/posts/3
 *   { "title": "고친 제목" }            ← 제목만 바뀌고 나머지는 그대로
 *
 * 작성 요청(PostCreateRequest)과 항목이 같지만 따로 만든 이유:
 *   수정은 "보낸 필드만 바꾸는" 부분 수정이라 규칙이 다르다. (P5)
 *   작성은 제목·내용이 필수지만, 수정은 모든 필드가 선택이다.
 *
 * 필드별 의미
 *   - 보내지 않음(null) : 원래 값을 그대로 둔다
 *   - 값을 보냄         : 그 값으로 바꾼다. URL·태그는 보낸 목록으로 통째로 교체한다
 *   - 빈 배열 []        : URL·태그를 모두 지운다
 *
 * 그래서 @NotBlank를 쓰지 않는다. (@NotBlank는 null도 거부하기 때문)
 * 대신 @Size(min = 1)로 "보냈다면 비어 있으면 안 된다"를 건다.
 * 공백만 보낸 제목은 compact constructor의 trim으로 ""이 되어 min = 1에 걸린다.
 */
public record PostUpdateRequest(

        @Schema(description = "바꿀 제목. 보내지 않으면 그대로 둔다. 보낸다면 1~100자(앞뒤 공백 제외).",
                example = "수정한 제목")
        @Size(min = 1, max = PostPolicy.TITLE_MAX, message = "제목은 1~100자로 입력해주세요.")
        String title,

        @Schema(description = "바꿀 본문. 보내지 않으면 그대로 둔다. 보낸다면 1~1000자(앞뒤 공백 제외).",
                example = "내용을 다시 정리했다.")
        @Size(min = 1, max = PostPolicy.CONTENT_MAX, message = "내용은 1~1000자로 입력해주세요.")
        String content,

        @Schema(description = "바꿀 참고 링크 목록. 보내지 않으면 그대로 둔다. 보낸다면 그 목록으로 통째로 교체하고, 빈 배열이면 모두 지운다. "
                + "최대 5개, 각 항목은 `http://` 또는 `https://` 로 시작하는 2,048자 이하(D-08).",
                example = "[\"https://example.com/note/1\"]")
        @Size(max = PostPolicy.URL_MAX_COUNT, message = "URL은 5개까지 넣을 수 있습니다.")
        List<@NotBlank(message = "URL은 빈 값일 수 없습니다.")
             @Size(max = PostPolicy.URL_MAX_LENGTH, message = "URL은 2048자 이하로 입력해주세요.")
             @Pattern(regexp = PostPolicy.URL_REGEX, message = "URL은 http:// 또는 https://로 시작해야 합니다.") String> urls,

        @Schema(description = "바꿀 해시태그 목록. 보내지 않으면 그대로 둔다. 보낸다면 그 목록으로 통째로 교체하고, 빈 배열이면 모두 지운다. "
                + "다듬는 규칙과 에러는 작성과 같다(`HASHTAG_LIMIT_EXCEEDED`, `INVALID_INPUT`).",
                example = "[\"여행\"]")
        List<@NotBlank(message = "해시태그는 빈 값일 수 없습니다.") String> hashtags
) {

    // 앞뒤 공백만 지운다. null은 "그대로 두기"라는 뜻이라 빈 목록으로 바꾸지 않는다.
    public PostUpdateRequest {
        title = UserPolicy.trim(title);
        content = UserPolicy.trim(content);
        urls = PostPolicy.trimAll(urls);
    }
}
