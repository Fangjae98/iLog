package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;

import java.time.LocalDateTime;
import java.util.List;

/*
 * [목록 응답용 DTO] 내 게시글 조회 같은 "여러 글을 나열하는" 화면에서 쓴다.
 *
 * 상세 조회용 PostResponse와 따로 만든 이유:
 *   목록에는 본문 전체가 필요 없다. 글이 많아질수록 본문까지 다 실어 보내면 응답이 무거워진다.
 *   목록은 제목만 보여 주고, 눌렀을 때 상세 조회 API로 본문을 가져오는 구조.
 *
 *   PostResponse        : id, memberId, 제목, 본문, urls, 태그, 작성일, 수정일   (상세)
 *   PostSummaryResponse : id, 제목, 태그, 닉네임, 작성일                        (목록)
 */
public record PostSummaryResponse(
        Long id,                  // 게시글 번호. 상세 조회(/posts/{id})로 넘어갈 때 쓴다
        String title,
        List<String> hashtags,
        String nickname,          // 작성자 닉네임
        LocalDateTime createdAt
) {

    public static PostSummaryResponse fromEntity(Post post) {
        return new PostSummaryResponse(
                post.getId(),
                post.getTitle(),
                post.getHashtagNames(),
                // TODO 닉네임은 Member 엔티티(B 담당)가 생기면 채운다.
                //      지금은 Post에 memberId(숫자)만 있어서 닉네임을 알 수 없으므로 null.
                null,
                post.getCreatedAt()
        );
    }
}
