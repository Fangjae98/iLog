package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [목록 응답용 DTO] 목록·검색·내 글처럼 "여러 글을 나열하는" 화면에서 쓴다.
 *
 * 상세 조회용 PostResponse와 따로 만든 이유:
 *   목록에는 본문 전체가 필요 없다. 글이 많아질수록 본문까지 다 실어 보내면 응답이 무거워진다.
 *   목록은 제목만 보여 주고, 눌렀을 때 상세 조회 API로 본문을 가져오는 구조.
 *
 *   PostResponse        : postId, 제목, 본문, urls, 태그, 작성자, isMine, 작성일, 수정일 (상세)
 *   PostSummaryResponse : postId, 제목, 닉네임, 태그, 작성일                         (목록)
 */
public record PostSummaryResponse(

        @Schema(description = "게시글 번호. 상세 조회(`GET /api/v1/posts/{postId}`)에 쓴다.", example = "12")
        Long postId,              // 게시글 번호. 목록 행을 눌러 상세로 넘어갈 때 쓴다 (S5)

        @Schema(description = "게시글 제목", example = "오늘 배운 Spring Data JPA")
        String title,

        @Schema(description = "작성자 닉네임", example = "기택")
        String nickname,          // 작성자 닉네임. 목록 조회가 작성자를 JOIN으로 함께 가져온다

        @Schema(description = "해시태그 목록. 없으면 빈 배열이다.", example = "[\"spring\",\"jpa\"]")
        List<String> hashtags,

        @Schema(description = "작성 일시 (KST)", example = "2026-09-20T15:00:00")
        LocalDateTime createdAt
) {

    public static PostSummaryResponse fromEntity(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getUser().getNickname(),
                post.getHashtagNames(),   // 글 여러 개의 태그를 @BatchSize로 한 번에 조회한다
                post.getCreatedAt()
        );
    }
}
