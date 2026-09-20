package com.ilog.ilog.post.repository;

import com.ilog.ilog.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/*
 * [게시글 작성 흐름] ⑥ Repository  ← 지금 이 파일
 *
 *   ④ Service → ⑤ Entity → ⑥ Repository → DB
 *
 * Repository = DB에 접근하는 창구. SQL을 직접 쓰지 않고 메서드 호출로 DB를 다룬다.
 *
 * JpaRepository<Post, Long> 을 상속하기만 하면
 *   - Post       : 다룰 엔티티
 *   - Long       : 그 엔티티의 @Id 타입
 * 아래 메서드들을 Spring Data JPA가 구현 없이 자동으로 만들어 준다.
 *
 *   save(post)         → INSERT (새 글 저장)  ← 게시글 작성에서 사용
 *   findById(id)       → SELECT ... WHERE id = ?
 *   findAll()          → SELECT 전체
 *   delete(post)       → DELETE
 *
 * 인터페이스인데 구현 클래스를 안 만들어도 되는 이유:
 *   앱이 실행될 때 Spring이 구현체를 대신 만들어서 Service에 넣어 준다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /*
     * 내 게시글 조회에 쓰는 메서드.
     *
     * 메서드 이름만 규칙대로 지으면 Spring Data JPA가 SQL을 대신 만들어 준다.
     *   findBy + UserId                → WHERE user_id = ?
     *   OrderBy + CreatedAt + Desc     → ORDER BY created_at DESC (최신 글이 위로)
     *
     * Pageable(몇 번째 쪽, 몇 개씩)을 넘기면 LIMIT / OFFSET까지 붙여서
     * "몇 쪽짜리인지, 전체 몇 건인지"를 담은 Page로 돌려준다.
     */
    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
