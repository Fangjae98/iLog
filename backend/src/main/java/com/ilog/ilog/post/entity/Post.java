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

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 전용. 외부에서는 빌더로만 생성
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Member 엔티티가 생기면 @ManyToOne 연관관계로 교체
    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Builder
    private Post(Long memberId, String title, String content, String url) {
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.url = url;
    }
}
