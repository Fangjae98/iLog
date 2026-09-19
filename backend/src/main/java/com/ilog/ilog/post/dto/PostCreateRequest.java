package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.entity.Post;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/*
 * [게시글 작성 흐름] ③ Request DTO  ← 지금 이 파일
 *
 *   ① Client → ② Controller → ③ Request DTO → ④ Service → ⑤ Entity → ⑥ Repository → DB
 *
 * DTO(Data Transfer Object) = 데이터를 "옮기는" 용도의 상자.
 * 클라이언트가 보낸 JSON이 이 객체로 자동 변환된다.
 *
 *   { "title": "첫 글", "content": "안녕하세요", "urls": ["https://a.com"], "hashtags": ["여행"] }
 *        ↓ (Spring이 자동 변환)
 *   new PostCreateRequest("첫 글", "안녕하세요", List.of("https://a.com"), List.of("여행"))
 *
 * 엔티티(Post)를 직접 받지 않는 이유:
 *   엔티티에는 id, memberId처럼 클라이언트가 마음대로 정하면 안 되는 값이 있다.
 *   DTO에는 "사용자가 입력하는 값"만 두어서 받을 수 있는 값을 제한한다.
 *
 * record = 값을 담기만 하는 클래스를 짧게 쓰는 문법.
 *   필드, 생성자, getter(title(), content()...)가 자동으로 만들어지고, 값은 바꿀 수 없다.
 */
public record PostCreateRequest(

        // 아래 검증 어노테이션은 Controller에서 @Valid를 붙였을 때 동작한다.
        // 규칙을 어기면 400 에러가 나고, message가 응답의 errors[].reason에 담긴다.

        @NotBlank(message = "제목은 필수입니다.")               // null, "", "   " 모두 거부
        @Size(max = 100, message = "제목은 100자 이하로 입력해주세요.")   // 엔티티의 length = 100과 맞춤
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        String content,

        // URL 여러 개. JSON에서는 "urls": ["https://a.com", "https://b.com"] 형태로 보낸다.
        // 목록 자체는 선택 입력(안 보내도 됨). 보낸다면 각 항목은 빈 문자열이면 안 된다.
        // <@NotBlank String> = "목록 안의 각 문자열"에 규칙을 거는 문법
        // TODO D-08 최대 개수 확정되면 @Size(max = N, message = "...")를 추가
        List<@NotBlank(message = "URL은 빈 값일 수 없습니다.") String> urls,

        // 해시태그. JSON에서는 "hashtags": ["여행", "맛집"] 형태로 보낸다. 안 보내도 됨.
        // 최대 개수(10개) 검사는 여기가 아니라 Service에서 한다.
        // 이유: 팀이 만들어 둔 전용 에러 코드(HASHTAG_LIMIT_EXCEEDED)로 응답하기 위해서.
        // (여기서 @Size로 막으면 일반 입력값 오류인 INVALID_INPUT으로 나간다)
        List<@NotBlank(message = "해시태그는 빈 값일 수 없습니다.") String> hashtags
) {

    // DTO → Entity 변환. Service에서 호출한다.
    // memberId는 요청 JSON이 아니라 로그인 정보에서 꺼내 넘겨받는다.
    // (JSON으로 받으면 남의 회원 번호를 넣어서 대신 글을 쓸 수 있기 때문)
    //
    // hashtags를 파라미터로 따로 받는 이유:
    //   Service에서 '#' 제거·중복 제거 등으로 다듬은 태그 목록을 넘겨주기 때문.
    public Post toEntity(Long memberId, List<String> refinedHashtags) {
        return Post.builder()
                .memberId(memberId)
                .title(title)
                .content(content)
                .urls(urls)
                .hashtags(refinedHashtags)
                .build();
    }
}
