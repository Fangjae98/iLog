package com.ilog.ilog.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 게시글 작성 응답 (P7).
 *
 * 새 글 번호만 돌려준다. 프론트는 이 번호로 상세 화면(GET /api/v1/posts/{postId})으로 이동한다.
 * 같은 주소가 Location 헤더에도 담긴다.
 */
public record PostCreateResponse(
        @Schema(description = "새로 만든 게시글 번호", example = "12")
        Long postId
) {
}
