package com.ilog.ilog.post.dto;

import com.ilog.ilog.post.domain.PostPolicy;
import com.ilog.ilog.post.entity.Post;
import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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
 *   엔티티에는 id, userId처럼 클라이언트가 마음대로 정하면 안 되는 값이 있다.
 *   DTO에는 "사용자가 입력하는 값"만 두어서 받을 수 있는 값을 제한한다.
 *
 * record = 값을 담기만 하는 클래스를 짧게 쓰는 문법.
 *   필드, 생성자, getter(title(), content()...)가 자동으로 만들어지고, 값은 바꿀 수 없다.
 */
public record PostCreateRequest(

        // 아래 검증 어노테이션은 Controller에서 @Valid를 붙였을 때 동작한다.
        // 규칙을 어기면 400 에러가 나고, message가 응답의 errors[].reason에 담긴다.
        // 숫자·정규식은 PostPolicy 한 곳에 모아 두고 수정(PostUpdateRequest)과 같이 쓴다. (P1, P2)

        @Schema(description = "게시글 제목. 필수, 100자 이하. 앞뒤 공백은 지운다. 비었거나 공백만 있으면 400 `INVALID_INPUT` 이 난다.",
                example = "오늘 배운 Spring Data JPA")
        @NotBlank(message = "제목은 필수입니다.")               // null, "", "   " 모두 거부
        @Size(max = PostPolicy.TITLE_MAX, message = "제목은 100자 이하로 입력해주세요.")   // 엔티티의 length = 100과 맞춤
        String title,

        @Schema(description = "게시글 본문. 필수, 1000자 이하. 앞뒤 공백은 지운다. 비었거나 공백만 있으면 400 `INVALID_INPUT` 이 난다.",
                example = "영속성 컨텍스트와 변경 감지를 정리했다.")
        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = PostPolicy.CONTENT_MAX, message = "내용은 1000자 이하로 입력해주세요.")
        String content,

        // URL 여러 개. JSON에서는 "urls": ["https://a.com", "https://b.com"] 형태로 보낸다.
        // 목록 자체는 선택 입력(안 보내면 빈 목록). 보낸다면 최대 5개, 각 항목은 http(s)://로 시작하는 2,048자 이하. (D-08)
        // <@NotBlank String> = "목록 안의 각 문자열"에 규칙을 거는 문법
        @Schema(description = "참고 링크 목록. 생략하거나 빈 배열로 보낼 수 있다. 최대 5개, 각 항목은 `http://` 또는 `https://` 로 시작하는 2,048자 이하(D-08).",
                example = "[\"https://docs.spring.io/spring-data/jpa/reference/\"]")
        @Size(max = PostPolicy.URL_MAX_COUNT, message = "URL은 5개까지 넣을 수 있습니다.")
        List<@NotBlank(message = "URL은 빈 값일 수 없습니다.")
             @Size(max = PostPolicy.URL_MAX_LENGTH, message = "URL은 2048자 이하로 입력해주세요.")
             @Pattern(regexp = PostPolicy.URL_REGEX, message = "URL은 http:// 또는 https://로 시작해야 합니다.") String> urls,

        // 해시태그. JSON에서는 "hashtags": ["여행", "맛집"] 형태로 보낸다. 안 보내도 됨.
        // 최대 개수(10개)와 글자 규칙 검사는 여기가 아니라 Service에서 한다.
        // 이유: '#' 제거·소문자·중복 제거로 다듬은 "뒤"의 값으로 세야 하고,
        //       개수 초과는 팀이 만들어 둔 전용 에러 코드(HASHTAG_LIMIT_EXCEEDED)로 응답하기 위해서. (P4)
        @Schema(description = "해시태그 목록. 생략하거나 빈 배열로 보낼 수 있다. 서버가 앞뒤 공백과 맨 앞 `#` 을 지우고 영문을 소문자로 바꾼 뒤 중복을 없앤다. "
                + "다듬은 뒤 개수가 10개를 넘으면 400 `HASHTAG_LIMIT_EXCEEDED`, "
                + "다듬은 태그가 1~20자의 한글·영문·숫자·`_` 가 아니면 400 `INVALID_INPUT` 이 난다(D-09).",
                example = "[\"여행\",\"맛집\"]")
        List<@NotBlank(message = "해시태그는 빈 값일 수 없습니다.") String> hashtags
) {

    // compact constructor : JSON이 이 record로 바뀔 때(검증보다 먼저) 한 번 실행된다.
    // 앞뒤 공백을 지운 값으로 검증·저장하고, 안 보낸 목록은 빈 목록으로 바꿔 둔다.
    public PostCreateRequest {
        title = UserPolicy.trim(title);
        content = UserPolicy.trim(content);
        urls = urls == null ? List.of() : PostPolicy.trimAll(urls);
        hashtags = hashtags == null ? List.of() : hashtags;
    }

    // DTO → Entity 변환. Service에서 호출한다.
    // 작성자(user)는 요청 JSON이 아니라 로그인 정보의 회원 번호로 Service가 만들어 넘긴다.
    // (JSON으로 받으면 남의 회원 번호를 넣어서 대신 글을 쓸 수 있기 때문)
    //
    // hashtags를 파라미터로 따로 받는 이유:
    //   Service에서 '#' 제거·중복 제거 등으로 다듬은 태그 목록을 넘겨주기 때문.
    public Post toEntity(User user, List<String> refinedHashtags) {
        return Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .urls(urls)
                .hashtags(refinedHashtags)
                .build();
    }
}
