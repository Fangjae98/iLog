package com.ilog.ilog.post.service;

import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.entity.Post;
import com.ilog.ilog.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * [게시글 작성 흐름] ④ Service  ← 지금 이 파일
 *
 *   ② Controller → ③ Request DTO → ④ Service → ⑤ Entity → ⑥ Repository → DB
 *   ② Controller ← ⑦ Response DTO ← ④ Service
 *
 * Service = 실제 "일"을 하는 곳 (비즈니스 로직).
 *   Controller는 요청을 받아서 넘겨주기만 하고,
 *   "DTO를 엔티티로 바꿔서 저장하고 결과를 돌려준다" 같은 처리 순서는 여기서 정한다.
 *
 * 예외 처리도 여기서 한다.
 *   예) 글이 없으면  throw new BusinessException(ErrorCode.POST_NOT_FOUND);
 *   던지기만 하면 global의 GlobalExceptionHandler가 알아서 에러 JSON으로 바꿔 준다.
 *   (작성 기능은 입력값 검증을 DTO가 처리하므로 여기서 던질 예외가 아직 없다.)
 */
@Service                     // Spring이 이 클래스를 객체로 만들어 관리(Bean 등록) → Controller에 주입 가능
@RequiredArgsConstructor     // final 필드를 받는 생성자 자동 생성 → Spring이 PostRepository를 넣어 줌 (생성자 주입)
@Transactional(readOnly = true)   // 기본은 "읽기 전용" 트랜잭션. 데이터를 바꾸는 메서드에만 따로 @Transactional을 붙인다
public class PostService {

    // new PostRepository() 하지 않아도 Spring이 만들어 둔 구현체를 넣어 준다.
    private final PostRepository postRepository;

    /**
     * 게시글 작성
     *
     * @param memberId 로그인한 회원 번호 (Controller가 로그인 정보에서 꺼내 전달)
     * @param request  사용자가 입력한 제목/내용/url
     * @return 저장된 게시글 정보
     */
    @Transactional   // 쓰기 작업이라 readOnly를 풀어 줌. 중간에 예외가 나면 DB 변경이 모두 취소(롤백)된다
    public PostResponse create(Long memberId, PostCreateRequest request) {
        // 1. DTO → Entity 변환 (아직 DB에 저장되기 전, id가 없는 상태)
        Post post = request.toEntity(memberId);

        // 2. DB에 저장 → INSERT 실행.
        //    이 순간 DB가 id를 매기고, created_at이 자동으로 채워진다.
        Post savedPost = postRepository.save(post);

        // 3. Entity → Response DTO 변환 후 Controller로 반환
        return PostResponse.fromEntity(savedPost);
    }
}
