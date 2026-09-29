package com.ilog.ilog.post.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.ilog.ilog.post.entity.Post;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [게시글 상세·수정 응답] ⑦ Response DTO  ← 지금 이 파일
 *
 *   ⑧ Client ← ② Controller ← ⑦ Response DTO ← ④ Service ← 저장된 Entity
 *
 * 저장이 끝난 Post 엔티티를 클라이언트에게 돌려줄 JSON 모양으로 바꾸는 상자.
 *
 *   Post 엔티티  →  PostResponse  →  { "postId": 1, "title": "첫 글", ..., "author": {...}, "isMine": true, ... }
 *
 * 엔티티를 그대로 응답하지 않는 이유:
 *   1) 엔티티에 필드가 추가되면 의도치 않게 응답에도 노출된다. (예: 나중에 생길 비밀 정보)
 *   2) 응답 모양을 DB 구조와 따로 관리할 수 있다. (예: 작성자 닉네임 추가)
 */
public record PostResponse(

        @Schema(description = "게시글 번호", example = "12")
        Long postId,

        @Schema(description = "게시글 제목", example = "오늘 배운 Spring Data JPA")
        String title,

        @Schema(description = "게시글 본문", example = "영속성 컨텍스트와 변경 감지를 정리했다.")
        String content,

        @Schema(description = "참고 링크 목록. 없으면 빈 배열이다.",
                example = "[\"https://docs.spring.io/spring-data/jpa/reference/\"]")
        List<String> urls,           // URL이 없으면 빈 목록 []

        @Schema(description = "해시태그 목록. 맨 앞 `#` 을 떼고 영문은 소문자로 다듬은 이름만 담는다. 없으면 빈 배열이다.",
                example = "[\"spring\",\"jpa\"]")
        List<String> hashtags,       // 태그가 없으면 빈 목록 []

        @Schema(description = "작성자")
        PostAuthorResponse author,

        // record의 isMine()을 Jackson이 "mine" 속성으로 읽지 않도록 JSON 이름을 못 박는다.
        // 프론트가 post.isMine 으로 읽는다. (P7)
        @Schema(description = "로그인한 회원이 쓴 글인지. 수정·삭제 버튼 표시에 쓴다.", example = "true")
        @JsonProperty("isMine")
        boolean isMine,

        @Schema(description = "작성 일시 (KST)", example = "2026-09-20T15:00:00")
        LocalDateTime createdAt,

        @Schema(description = "수정 일시 (KST). 한 번도 수정하지 않았으면 null 이다.",
                nullable = true, example = "2026-09-21T09:30:00")
        LocalDateTime updatedAt      // 처음 작성할 때는 null (수정해야 값이 생김)
) {

    /**
     * @param post        응답으로 바꿀 글. 작성자(user)를 함께 읽는다
     * @param loginUserId 지금 로그인한 회원 번호. 작성자와 같으면 isMine = true
     */
    public static PostResponse fromEntity(Post post, Long loginUserId) {
        return new PostResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                List.copyOf(post.getUrls()),
                post.getHashtagNames(),   // PostHashtag 객체 목록 → 이름 목록 (["spring", "jpa"])
                PostAuthorResponse.from(post.getUser()),
                post.isOwner(loginUserId),
                post.getCreatedAt(),   // BaseTimeEntity에서 물려받은 getter
                post.getUpdatedAt()
        );
    }
}
