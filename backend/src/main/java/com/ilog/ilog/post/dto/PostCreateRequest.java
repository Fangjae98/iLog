package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 게시글 작성 요청. 작성자(memberId)는 요청 바디가 아닌 로그인 정보에서 받는다. */
public record PostCreateRequest(

        @NotBlank
        @Size(max = 100)
        String title,

        @NotBlank
        String content,

        String url
) {

    public Post toEntity(Long memberId) {
        return Post.builder()
                .memberId(memberId)
                .title(title)
                .content(content)
                .url(url)
                .build();
    }
}
