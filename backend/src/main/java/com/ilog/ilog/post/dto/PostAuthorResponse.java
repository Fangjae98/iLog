package com.ilog.ilog.post.dto;

import com.ilog.ilog.user.domain.User;
import io.swagger.v3.oas.annotations.media.Schema;

/** 게시글 작성자 정보 (P7). 상세 화면이 {@code post.author.nickname} 으로 읽는다. */
public record PostAuthorResponse(
        @Schema(description = "작성자 회원 번호", example = "1")
        Long userId,
        @Schema(description = "작성자 닉네임", example = "기택")
        String nickname
) {

    public static PostAuthorResponse from(User user) {
        return new PostAuthorResponse(user.getId(), user.getNickname());
    }
}
