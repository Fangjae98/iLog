package com.ilog.ilog.post.dto;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class postDto{
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
