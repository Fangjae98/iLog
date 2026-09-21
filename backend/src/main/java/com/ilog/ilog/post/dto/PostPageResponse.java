package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/*
 * [페이징 응답 상자] 목록을 쪽(page) 단위로 잘라서 돌려줄 때 쓰는 형태.
 *
 *   {
 *     "content": [ {게시글 요약}, {게시글 요약}, ... ],   ← 이번 쪽에 담긴 글들
 *     "page": 0,              ← 지금 몇 번째 쪽인지 (0부터 시작)
 *     "size": 5,              ← 한 쪽에 몇 개씩
 *     "totalElements": 12,    ← 전체 글 수
 *     "totalPages": 3,        ← 전체 쪽 수
 *     "hasNext": true         ← 다음 쪽이 있는지 (프론트의 '더 보기' 버튼 판단용)
 *   }
 *
 * Spring이 주는 Page 객체를 그대로 응답하지 않는 이유:
 *   Page를 그대로 JSON으로 바꾸면 우리가 쓰지 않는 필드까지 잔뜩 붙고,
 *   Spring 버전이 올라가면 모양이 바뀔 수 있어 프론트가 깨질 수 있다.
 *
 * TODO 페이징 응답 형식은 D-01~05(A 담당)에서 팀 표준이 정해지면 그쪽에 맞춘다.
 *      공통으로 쓰기로 하면 global 패키지로 옮길 수 있다.
 */
public record PostPageResponse(
        // example 을 두지 않는 이유: 항목이 PostSummaryResponse 객체라 예시가 그쪽과 중복되고 같이 관리해야 한다.
        @Schema(description = "이번 쪽에 담긴 게시글 요약 목록")
        List<PostSummaryResponse> content,

        @Schema(description = "현재 쪽 번호. 0부터 시작한다.", example = "0")
        int page,

        @Schema(description = "한 쪽에 담기는 게시글 수. 내 게시글 조회는 5로 고정이다.", example = "5")
        int size,

        @Schema(description = "조건에 맞는 전체 게시글 수", example = "12")
        long totalElements,

        @Schema(description = "전체 쪽 수", example = "3")
        int totalPages,

        @Schema(description = "다음 쪽이 있는지. 프론트의 '더 보기' 버튼 판단에 쓴다.", example = "true")
        boolean hasNext
) {

    public static PostPageResponse fromPage(Page<Post> postPage) {
        return new PostPageResponse(
                // map: 게시글 엔티티 목록 → 요약 DTO 목록
                postPage.getContent().stream()
                        .map(PostSummaryResponse::fromEntity)
                        .toList(),
                postPage.getNumber(),
                postPage.getSize(),
                postPage.getTotalElements(),
                postPage.getTotalPages(),
                postPage.hasNext()
        );
    }
}
