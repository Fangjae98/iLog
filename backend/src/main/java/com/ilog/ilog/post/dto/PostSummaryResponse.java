package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [목록 응답용 DTO] 내 게시글 조회 같은 "여러 글을 나열하는" 화면에서 쓴다.
 *
 * 상세 조회용 PostResponse와 따로 만든 이유:
 *   목록에는 본문 전체가 필요 없다. 글이 많아질수록 본문까지 다 실어 보내면 응답이 무거워진다.
 *   목록은 제목만 보여 주고, 눌렀을 때 상세 조회 API로 본문을 가져오는 구조.
 *
 *   PostResponse        : id, userId, 제목, 본문, urls, 태그, 작성일, 수정일     (상세)
 *   PostSummaryResponse : id, 제목, 태그, 닉네임, 작성일                        (목록)
 */
public record PostSummaryResponse(
        @Schema(description = "게시글 번호. 상세 조회(`GET /api/v1/posts/{postId}`)에 쓴다.", example = "1")
        Long id,                  // 게시글 번호. 상세 조회(/posts/{id})로 넘어갈 때 쓴다

        @Schema(description = "게시글 제목", example = "오늘 배운 Spring Data JPA")
        String title,

        @Schema(description = "해시태그 목록. 없으면 빈 배열이다.", example = "[\"여행\",\"맛집\"]")
        List<String> hashtags,

        // example 을 두지 않는 이유: fromEntity 가 항상 null 을 넣고 있어서 예시를 쓰면 현재 동작과 어긋난다.
        @Schema(description = "작성자 닉네임. `User` 엔티티와 연결하기 전까지는 항상 null 이다.", nullable = true)
        String nickname,          // 작성자 닉네임

        @Schema(description = "작성 일시 (KST)", example = "2026-09-20T15:00:00")
        LocalDateTime createdAt
) {

    public static PostSummaryResponse fromEntity(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getHashtagNames(),
                // TODO 닉네임은 User 엔티티(B 담당, feat/be/user-revise)가 be에 합쳐지면 채운다.
                //      지금은 Post에 userId(숫자)만 있어서 닉네임을 알 수 없으므로 null.
                null,
                post.getCreatedAt()
        );
    }
}
