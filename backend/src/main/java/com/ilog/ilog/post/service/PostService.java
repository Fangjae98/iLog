package com.ilog.ilog.post.service;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.entity.Post;
import com.ilog.ilog.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/*
 * [게시글 작성 흐름] ④ Service  ← 지금 이 파일
 *
 *   ② Controller → ③ Request DTO → ④ Service → ⑤ Entity → ⑥ Repository → DB
 *   ② Controller ← ⑦ Response DTO ← ④ Service
 *
 * Service = 실제 "일"을 하는 곳 (비즈니스 로직).
 *   Controller는 요청을 받아서 넘겨주기만 하고,
 *   "태그를 다듬고 → 개수를 확인하고 → 저장하고 → 결과를 돌려준다" 같은 처리 순서는 여기서 정한다.
 *
 * 예외도 여기서 던진다.
 *   예) throw new BusinessException(ErrorCode.HASHTAG_LIMIT_EXCEEDED);
 *   던지기만 하면 global의 GlobalExceptionHandler가 알아서 에러 JSON으로 바꿔 준다.
 */
@Service                     // Spring이 이 클래스를 객체로 만들어 관리(Bean 등록) → Controller에 주입 가능
@RequiredArgsConstructor     // final 필드를 받는 생성자 자동 생성 → Spring이 PostRepository를 넣어 줌 (생성자 주입)
@Transactional(readOnly = true)   // 기본은 "읽기 전용" 트랜잭션. 데이터를 바꾸는 메서드에만 따로 @Transactional을 붙인다
public class PostService {

    /** 글 하나에 붙일 수 있는 해시태그 최대 개수 (D-09 확정) */
    private static final int HASHTAG_MAX_COUNT = 10;

    // new PostRepository() 하지 않아도 Spring이 만들어 둔 구현체를 넣어 준다.
    private final PostRepository postRepository;

    /**
     * 게시글 작성
     *
     * @param memberId 로그인한 회원 번호 (Controller가 로그인 정보에서 꺼내 전달)
     * @param request  사용자가 입력한 제목/내용/url/해시태그
     * @return 저장된 게시글 정보
     */
    @Transactional   // 쓰기 작업이라 readOnly를 풀어 줌. 중간에 예외가 나면 DB 변경이 모두 취소(롤백)된다
    public PostResponse create(Long memberId, PostCreateRequest request) {
        // 1. 해시태그 다듬기 + 개수 검사
        List<String> hashtags = refineHashtags(request.hashtags());

        // 2. DTO → Entity 변환 (아직 DB에 저장되기 전, id가 없는 상태)
        Post post = request.toEntity(memberId, hashtags);

        // 3. DB에 저장 → INSERT 실행.
        //    이 순간 DB가 id를 매기고, created_at이 자동으로 채워진다.
        //    태그는 Post의 cascade 설정 덕분에 post_hashtag 표에 함께 저장된다.
        Post savedPost = postRepository.save(post);

        // 4. Entity → Response DTO 변환 후 Controller로 반환
        return PostResponse.fromEntity(savedPost);
    }

    /**
     * 해시태그 다듬기
     *
     * 화면(프론트)에서도 '#' 제거·중복 제거를 하지만, 서버로는 어떤 값이든 들어올 수 있으므로
     * 저장 직전에 한 번 더 정리한다. (프론트를 거치지 않는 요청도 가능하기 때문)
     *
     *   입력:  ["#여행", " 맛집 ", "여행", ""]
     *   결과:  ["여행", "맛집"]
     */
    private List<String> refineHashtags(List<String> rawHashtags) {
        if (rawHashtags == null) {
            return List.of();   // 태그를 안 보냈으면 빈 목록
        }

        List<String> refined = rawHashtags.stream()
                .map(String::trim)                        // 앞뒤 공백 제거:  " 맛집 " → "맛집"
                .map(tag -> tag.replaceAll("^#+", ""))    // 맨 앞의 # 제거:  "#여행" → "여행"
                .map(String::trim)                        // "# 여행"처럼 # 뒤에 공백이 있던 경우 한 번 더
                .filter(tag -> !tag.isEmpty())            // 빈 문자열 제외
                .distinct()                               // 중복 제거:      ["여행", "여행"] → ["여행"]
                .toList();

        // 다듬은 뒤의 개수로 검사한다. (중복을 제거하기 전 개수로 막으면 사용자에게 불리)
        if (refined.size() > HASHTAG_MAX_COUNT) {
            throw new BusinessException(ErrorCode.HASHTAG_LIMIT_EXCEEDED);
        }
        return refined;
    }
}
