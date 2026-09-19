package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;

import java.time.LocalDateTime;

/** 게시글 응답. 엔티티를 그대로 내보내지 않고 이 DTO로 변환해서 반환한다. */
public record PostResponse(
        Long id,
        Long memberId,
        String title,
        String content,
        String url,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PostResponse fromEntity(Post post) {
        return new PostResponse(
                post.getId(),
                post.getMemberId(),
                post.getTitle(),
                post.getContent(),
                post.getUrl(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
