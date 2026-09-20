package com.ilog.ilog.post.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * [게시글 작성 흐름] ⑤ Entity (해시태그)  ← 지금 이 파일
 *
 * 게시글에 붙는 태그 하나 = 이 객체 하나 = post_hashtag 표의 한 줄.
 *
 *   posts                      post_hashtag
 *   id | title                 id | post_id | name
 *   1  | 첫 글                 1  | 1       | 여행
 *   2  | 둘째 글               2  | 1       | 맛집
 *                              3  | 2       | 여행   ← 글마다 태그 글자를 따로 저장 (D-09: 단일 테이블 방식)
 *
 * 태그는 글 하나에 여러 개 붙으므로 posts 표의 한 칸에 담을 수 없어 별도 표로 뺐다.
 * URL(post_url)과 달리 엔티티로 만든 이유:
 *   검색 담당(A)이 이 표를 직접 조회해야 하고, 나중에 태그에 속성이 붙을 수도 있어서.
 */
@Entity
@Table(
        name = "post_hashtag",
        // 인덱스 = 책의 색인. "여행 태그가 붙은 글 찾기"처럼 name으로 검색할 때 빨라진다.
        // 목록·검색 담당(A)이 태그로 거르는 쿼리를 쓰기 때문에 미리 걸어 둔다.
        indexes = @Index(name = "idx_post_hashtag_name", columnList = "name")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이 태그가 어느 글의 것인지. @ManyToOne = "태그 여러 개 → 글 하나"
    // fetch = LAZY : 태그를 조회할 때 글까지 자동으로 같이 불러오지 않음 (필요할 때만 조회 → 불필요한 SQL 방지)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)   // post_hashtag 표의 post_id 컬럼으로 연결
    private Post post;

    // 태그 이름. '#'은 빼고 글자만 저장한다. (예: "#여행" → "여행")
    // TODO D-09 태그 글자 수 제한이 확정되면 length 조정
    @Column(nullable = false, length = 50)
    private String name;

    // Post가 태그를 만들 때만 쓰도록 생성자를 같은 패키지 안에서만 열어 둔다.
    PostHashtag(Post post, String name) {
        this.post = post;
        this.name = name;
    }
}
