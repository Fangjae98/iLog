package com.ilog.ilog.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/*
 * [게시글 수정 요청 DTO]
 *
 *   PUT /api/v1/posts/3
 *   { "title": "고친 제목", "content": "고친 내용", "urls": [...], "hashtags": [...] }
 *
 * 작성 요청(PostCreateRequest)과 항목이 같지만 따로 만든 이유:
 *   지금은 같아도 나중에 한쪽만 규칙이 바뀌는 일이 흔하다.
 *   (예: 수정할 때는 태그를 못 바꾸게 하기) 그때 서로 영향을 주지 않는다.
 *
 * URL·태그는 "보낸 목록으로 통째로 교체"된다.
 *   태그를 ["여행"]으로 보내면 원래 있던 "맛집"은 사라진다.
 *   태그를 아예 안 보내거나 []로 보내면 태그가 모두 지워진다.
 */
public record PostUpdateRequest(

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        List<@NotBlank(message = "URL은 빈 값일 수 없습니다.") String> urls,

        // 최대 10개 검사는 Service에서 (HASHTAG_LIMIT_EXCEEDED 에러 코드를 쓰기 위해)
        List<@NotBlank(message = "해시태그는 빈 값일 수 없습니다.") String> hashtags
) {
}
