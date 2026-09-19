package com.ilog.ilog.post;

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

// 자바 8부터 제공하는 날짜 및 시간 클래스입니다.
// 게시글 작성일(createdAt), 수정일(updatedAt) 등의 필드 데이터 타입으로 쓰입니다.
import java.time.LocalDateTime;

@Entity
@Table(name="posts")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class Post{
    private Bigint id;
    private Bigint member_id;
    private varchar(100) title;
    private text content;
    private text url;
    private timestamp created_at;
    private timestamp updated_at;

    public Post toEntity(){
        Post post = Post.builder()
                .id(id).user(member_id)
                .title(title)
                .content(content)
                .build();
        return post;
    }

    public static PostDto fromEntity(Post post){
    return PostDto.builder()
            .id(post.getId())
            .title(post.getTitle())
            .content(post.getContent())
            .member_id(post.getMember_id())
            .build();
    }

    @builder
    public PostDto(Bigint id,Bigint member_id, varchar title, text content, text url, timestamp createDate){
        this.id = id;
        this.member_id=member_id;
        this.title=title;
        this. content=content;
        this.url=url;
        this.created_at=created_at;
    }

}
