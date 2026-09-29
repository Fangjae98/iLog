package com.ilog.ilog.post.repository;

import com.ilog.ilog.post.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

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
public interface PostRepository extends JpaRepository<Post, Long>, JpaSpecificationExecutor<Post> {

    /*
     * 목록·검색·내 글 조회에 쓰는 메서드. (T15)
     *
     * JpaSpecificationExecutor가 만들어 주는 findAll(조건, 쪽 정보)을 그대로 쓰되,
     * @EntityGraph(user)를 붙여 목록의 작성자 닉네임을 글과 함께 JOIN으로 가져온다.
     * (안 붙이면 글 10개의 작성자를 따로 조회하는 SELECT가 10번 더 나간다 = N+1)
     *
     * 작성자는 글 하나에 한 명(ManyToOne)이라 JOIN해도 행 수가 늘지 않아서 쪽 나누기(LIMIT/OFFSET)와 같이 써도 안전하다.
     * 전체 개수를 세는 count 쿼리에는 이 JOIN이 붙지 않는다.
     * 첫 쪽 결과가 쪽 크기보다 적으면 전체 개수를 이미 알 수 있어 count 쿼리를 아예 실행하지 않는다.
     */
    @Override
    @EntityGraph(attributePaths = "user")
    Page<Post> findAll(Specification<Post> spec, Pageable pageable);

    /**
     * 글 하나를 작성자(User)와 함께 한 번에 조회한다.
     *
     * @EntityGraph(user) : posts와 users를 JOIN해서 SELECT 한 번으로 가져온다.
     * 상세·수정·삭제 모두 작성자의 탈퇴 여부(P8)와 닉네임(P7)이 필요해서,
     * findById 후 작성자를 따로 조회(SELECT 2번)하지 않도록 한다.
     */
    @EntityGraph(attributePaths = "user")
    Optional<Post> findWithUserById(Long id);
}
