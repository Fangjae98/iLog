package com.ilog.ilog.post.entity;

// 작성일(created_at), 수정일(updated_at)을 자동으로 채워주는 공통 부모 클래스입니다.
import com.ilog.ilog.global.entity.BaseTimeEntity;

// JPA(데이터베이스 자바 표준) 어노테이션들을 가져옵니다.
// @Entity, @Table, @Id, @GeneratedValue, @Column 등을 사용할 수 있게 해줍니다.
import jakarta.persistence.*;

// Lombok: 필드의 접근 제어자(public, protected, private 등)를 지정할 때 사용하는 옵션값입니다.
// 예: @NoArgsConstructor(access = AccessLevel.PROTECTED) 처럼 기본 생성자의 접근 범위를 제한할 때 씁니다.
import lombok.AccessLevel;

// Lombok: 빌더 패턴(Builder Pattern) 생성자를 자동으로 만들어주는 어노테이션입니다.
// 객체를 생성할 때 Post.builder().title("제목").content("내용").build() 형태로 안전하게 값을 넣을 수 있습니다.
import lombok.Builder;

// Lombok: 모든 필드의 Getter 메서드(getId(), getTitle() 등)를 자동으로 생성해 줍니다.
import lombok.Getter;

// Lombok: 파라미터(매개변수)가 전혀 없는 기본 생성자(예: public Post() {})를 자동으로 만들어 줍니다.
// JPA는 내부적으로 엔티티를 다룰 때 기본 생성자가 필수입니다.
import lombok.NoArgsConstructor;

// Hibernate(JPA 구현체): 연관된 데이터를 한 번에 모아서 조회하게 해 주는 어노테이션입니다.
import org.hibernate.annotations.BatchSize;

// 여러 개의 값을 순서대로 담는 목록(List)과 그 기본 구현체(ArrayList)입니다. URL 여러 개를 담을 때 씁니다.
import java.util.ArrayList;
import java.util.List;

/*
 * [게시글 작성 흐름] ⑤ Entity  ← 지금 이 파일
 *
 *   ① Client → ② Controller → ③ Request DTO → ④ Service → ⑤ Entity → ⑥ Repository → DB
 *                                                                               ↓
 *   ⑧ Client ← ② Controller ← ⑦ Response DTO ← ④ Service ←──────── 저장된 Entity
 *
 * 엔티티 = DB 테이블 한 줄(row)을 자바 객체로 표현한 것.
 * Post 객체 1개 = posts 테이블의 게시글 1개.
 * 이 클래스를 바탕으로 JPA가 INSERT/SELECT 같은 SQL을 대신 만들어 준다.
 */
@Entity                       // "이 클래스는 DB 테이블과 연결된 엔티티다"라고 JPA에게 알려줌
@Table(name = "posts")        // 연결할 테이블 이름. 없으면 클래스명(post)을 그대로 씀
@Getter                       // 값을 읽는 getId(), getTitle()... 자동 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 전용 기본 생성자. protected라 우리 코드에서는 new Post() 불가 → 빌더로만 생성
public class Post extends BaseTimeEntity {           // BaseTimeEntity를 상속 → created_at, updated_at 컬럼이 자동으로 따라옴

    @Id                                                   // 이 필드가 기본키(PK), 즉 게시글을 구분하는 고유 번호
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // 번호는 우리가 넣지 않고 DB가 1, 2, 3... 자동으로 매김
    private Long id;

    // 글쓴이의 회원 번호. 지금은 숫자만 저장하고,
    // Member 엔티티가 생기면 @ManyToOne 연관관계로 교체 예정
    @Column(name = "member_id", nullable = false)         // 자바 필드명은 memberId, DB 컬럼명은 member_id / NOT NULL
    private Long memberId;

    @Column(nullable = false, length = 100)               // VARCHAR(100) NOT NULL
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")  // 본문은 길 수 있으니 길이 제한 없는 TEXT 타입
    private String content;

    // 한 글에 URL이 여러 개 → posts 표의 한 칸에 담을 수 없으므로 별도의 표(post_url)에 한 줄씩 저장한다.
    //
    //   posts                         post_url
    //   id | title   | ...           post_id | sort_order | url
    //   1  | 첫 글   |               1       | 0          | https://a.com
    //                                 1       | 1          | https://b.com
    //
    // @ElementCollection  : "문자열 목록을 별도 표에 저장해 줘"라고 JPA에 알려줌 (URL은 문자열뿐이라 엔티티까지 만들 필요 없음)
    // @CollectionTable    : 그 표의 이름(post_url)과, 어느 글의 URL인지 가리키는 컬럼(post_id)
    // @OrderColumn        : 사용자가 입력한 순서를 기억하는 컬럼(sort_order). 없으면 조회할 때 순서가 섞일 수 있음
    // 게시글을 저장·삭제하면 URL도 같이 저장·삭제된다.
    @ElementCollection
    @CollectionTable(name = "post_url", joinColumns = @JoinColumn(name = "post_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "url", nullable = false, columnDefinition = "TEXT")
    private List<String> urls = new ArrayList<>();   // 빈 목록으로 시작 → URL을 안 넣어도 null이 아님

    // 이 글에 붙은 해시태그 목록. 실제 저장은 post_hashtag 표(PostHashtag 엔티티)에 한 줄씩.
    // mappedBy = "post"        : 연결 정보(post_id)는 PostHashtag 쪽 post 필드가 가지고 있다는 뜻
    // cascade = ALL            : 글을 저장/삭제하면 태그도 함께 저장/삭제 ("같이 움직인다")
    // orphanRemoval = true     : 이 목록에서 빼면 DB에서도 삭제 → 글 수정 시 태그 교체에 필요 (D-06)
    // @BatchSize : 목록 조회에서 글 5개의 태그를 가져올 때,
    //   글마다 따로 조회(SELECT 5번)하지 않고 한 번에 모아서 조회하게 한다. (N+1 문제 방지)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 100)
    private List<PostHashtag> hashtags = new ArrayList<>();

    // Setter가 없으므로 값은 이 생성자를 통해서만 넣을 수 있다.
    // @Builder 덕분에 Post.builder().title("...").build() 형태로 호출.
    // id와 작성/수정일은 DB와 JPA가 채우므로 파라미터에서 뺐다.
    @Builder
    private Post(Long memberId, String title, String content, List<String> urls, List<String> hashtags) {
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        // 받은 목록을 그대로 쓰지 않고 새 목록에 복사 → 바깥에서 원본 목록을 바꿔도 엔티티에 영향 없음
        if (urls != null) {
            this.urls = new ArrayList<>(urls);
        }
        // 태그는 글자(String)로 받아서, 표 한 줄에 해당하는 PostHashtag 객체로 바꿔 담는다.
        if (hashtags != null) {
            hashtags.forEach(name -> this.hashtags.add(new PostHashtag(this, name)));
        }
    }

    /**
     * 게시글 수정 (D-06 확정: 제목·본문·URL·태그 모두 수정 가능)
     *
     * Setter를 열어 두는 대신 이렇게 "무슨 일을 하는지 이름이 붙은" 메서드로만 값을 바꾼다.
     * 아무 데서나 post.setTitle(...)을 할 수 있으면, 나중에 값이 어디서 바뀌었는지 찾기 어려워진다.
     *
     * JPA는 트랜잭션 안에서 엔티티 값이 바뀌면 자동으로 UPDATE 문을 만들어 준다(변경 감지).
     * 그래서 이 메서드를 부르기만 하면 되고, 따로 save()를 부르지 않아도 된다.
     */
    public void update(String title, String content, List<String> urls, List<String> hashtags) {
        this.title = title;
        this.content = content;

        // URL·태그는 "일부만 바꾸기"가 아니라 "받은 목록으로 통째로 교체"한다.
        // 기존 것을 비우고 새로 담으면, 지워진 것은 DB에서도 삭제된다(orphanRemoval).
        this.urls.clear();
        if (urls != null) {
            this.urls.addAll(urls);
        }

        this.hashtags.clear();
        if (hashtags != null) {
            hashtags.forEach(name -> this.hashtags.add(new PostHashtag(this, name)));
        }
    }

    /** 이 글을 쓴 사람이 맞는지 확인한다. 수정·삭제 전에 Service에서 호출한다. */
    public boolean isOwner(Long memberId) {
        return this.memberId.equals(memberId);
    }

    /** 응답을 만들 때 쓰기 편하도록 태그 객체 목록을 이름 목록으로 바꿔 준다. (예: ["여행", "맛집"]) */
    public List<String> getHashtagNames() {
        return hashtags.stream()
                .map(PostHashtag::getName)
                .toList();
    }
}
