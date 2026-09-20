package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [게시글 작성 흐름] ⑦ Response DTO  ← 지금 이 파일
 *
 *   ⑧ Client ← ② Controller ← ⑦ Response DTO ← ④ Service ← 저장된 Entity
 *
 * 저장이 끝난 Post 엔티티를 클라이언트에게 돌려줄 JSON 모양으로 바꾸는 상자.
 *
 *   Post 엔티티  →  PostResponse  →  { "id": 1, "title": "첫 글", ..., "createdAt": "2026-09-20T15:00:00" }
 *
 * 엔티티를 그대로 응답하지 않는 이유:
 *   1) 엔티티에 필드가 추가되면 의도치 않게 응답에도 노출된다. (예: 나중에 생길 비밀 정보)
 *   2) 응답 모양을 DB 구조와 따로 관리할 수 있다. (예: 작성자 닉네임 추가)
 */
public record PostResponse(
        Long id,
        Long userId,
        String title,
        String content,
        List<String> urls,           // URL이 없으면 빈 목록 []
        List<String> hashtags,       // 태그가 없으면 빈 목록 []
        LocalDateTime createdAt,
        LocalDateTime updatedAt      // 처음 작성할 때는 null (수정해야 값이 생김)
) {

    // Entity → DTO 변환. static이라 PostResponse.fromEntity(post)로 바로 호출한다.
    public static PostResponse fromEntity(Post post) {
        return new PostResponse(
                post.getId(),
                post.getUserId(),
                post.getTitle(),
                post.getContent(),
                // urls는 post_url 표에 따로 있어서, 실제로 필요할 때 DB에서 읽어 온다(지연 로딩).
                // 그래서 이 변환은 Service의 @Transactional 메서드 안에서 호출해야 한다.
                // List.copyOf = JPA가 관리하는 목록을 일반 목록으로 복사
                List.copyOf(post.getUrls()),
                post.getHashtagNames(),   // PostHashtag 객체 목록 → 이름 목록 (["여행", "맛집"])
                post.getCreatedAt(),   // BaseTimeEntity에서 물려받은 getter
                post.getUpdatedAt()
        );
    }
}
