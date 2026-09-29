package com.ilog.ilog.post.service;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import com.ilog.ilog.post.domain.PostPolicy;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostCreateResponse;
import com.ilog.ilog.post.dto.PostPageResponse;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.dto.PostSearchRequest;
import com.ilog.ilog.post.dto.PostUpdateRequest;
import com.ilog.ilog.post.entity.Post;
import com.ilog.ilog.post.repository.PostRepository;
import com.ilog.ilog.post.repository.PostSpecs;
import com.ilog.ilog.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
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

    /** 목록 정렬: 최신 글이 위로, 작성 시각이 같으면 나중에 쓴 글(id가 큰 글)이 위로 (S4) */
    private static final Sort LATEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    // new PostRepository() 하지 않아도 Spring이 만들어 둔 구현체를 넣어 준다.
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    /**
     * 게시글 작성
     *
     * @param userId 로그인한 회원 번호 (Controller가 로그인 정보에서 꺼내 전달)
     * @param request  사용자가 입력한 제목/내용/url/해시태그
     * @return 새 글 번호 (P7: 프론트는 이 번호로 상세 화면으로 이동한다)
     */
    @Transactional   // 쓰기 작업이라 readOnly를 풀어 줌. 중간에 예외가 나면 DB 변경이 모두 취소(롤백)된다
    public PostCreateResponse create(Long userId, PostCreateRequest request) {
        // 1. 해시태그 다듬기 + 개수 검사
        List<String> hashtags = refineHashtags(request.hashtags());

        // 2. DTO → Entity 변환 (아직 DB에 저장되기 전, id가 없는 상태)
        // getReferenceById : 회원을 SELECT 하지 않고 id만 담은 참조(프록시)를 만든다.
        // 글을 저장할 때 user_id 값만 있으면 되므로 회원 전체를 조회할 필요가 없다. (P9)
        Post post = request.toEntity(userRepository.getReferenceById(userId), hashtags);

        // 3. DB에 저장 → INSERT 실행.
        //    이 순간 DB가 id를 매기고, created_at이 자동으로 채워진다.
        //    태그는 Post의 cascade 설정 덕분에 post_hashtag 표에 함께 저장된다.
        Post savedPost = postRepository.save(post);

        // 4. 새 글 번호만 담아 Controller로 반환
        return new PostCreateResponse(savedPost.getId());
    }

    /**
     * 게시글 상세 조회
     *
     * 클래스에 붙은 @Transactional(readOnly = true)가 그대로 적용된다.
     * 읽기만 하므로 따로 @Transactional을 붙이지 않는다.
     *
     * @param loginUserId 로그인한 회원 번호. 내 글인지(isMine) 판단에 쓴다
     * @param postId      조회할 게시글 번호
     * @return 게시글 정보 + 작성자 + isMine
     * @throws BusinessException 글이 없거나 작성자가 탈퇴했으면 POST_NOT_FOUND(404)
     */
    public PostResponse getPost(Long loginUserId, Long postId) {
        Post post = findVisiblePost(postId);

        // urls, hashtags는 필요할 때 DB에서 읽어 오는(지연 로딩) 값이라
        // 이 변환은 반드시 트랜잭션 안(= 이 메서드 안)에서 해야 한다.
        return PostResponse.fromEntity(post, loginUserId);
    }

    /**
     * 게시글 목록·검색·내 글 조회 (PST-02, FN-PST-006, S1~S5)
     *
     * 받은 조건만 골라 모두 만족하는 글(AND)을 최신순으로 한 쪽씩 돌려준다.
     * 작성자가 탈퇴한 글은 조건과 상관없이 항상 뺀다. (P8)
     *
     * @param loginUserId 로그인한 회원 번호. author=me일 때 이 사람이 쓴 글만 찾는다
     * @param request     검색 조건과 쪽 정보
     * @return 이번 쪽의 글 목록 + 전체 개수 같은 쪽 정보
     */
    public PostPageResponse search(Long loginUserId, PostSearchRequest request) {
        List<Specification<Post>> conditions = new ArrayList<>();
        conditions.add(PostSpecs.authorActive());

        if (request.keyword() != null) {
            conditions.add(PostSpecs.keyword(request.keyword()));
        }
        if (request.title() != null) {
            conditions.add(PostSpecs.titleContains(request.title()));
        }
        if (request.nickname() != null) {
            conditions.add(PostSpecs.nickname(request.nickname()));
        }
        List<String> tags = searchHashtags(request.hashtag());
        if (!tags.isEmpty()) {
            conditions.add(PostSpecs.hasAllTags(tags));
        }
        if (request.date() != null) {
            conditions.add(PostSpecs.createdOn(request.date()));
        }
        if (request.onlyMine()) {
            conditions.add(PostSpecs.writtenBy(loginUserId));
        }

        // PageRequest.of(쪽 번호, 개수, 정렬) = "몇 번째 쪽을 몇 개씩, 어떤 순서로 달라"는 주문서
        // 작성 시각이 같은 글이 있어도 순서가 흔들리지 않게 id를 두 번째 기준으로 둔다. (S4)
        Page<Post> posts = postRepository.findAll(Specification.allOf(conditions),
                PageRequest.of(request.page(), request.size(), LATEST_FIRST));

        return PostPageResponse.fromPage(posts);
    }

    /**
     * 검색용 해시태그 다듬기.
     * 저장과 같은 규칙으로 다듬어야 "#Spring"으로 검색해도 "spring" 태그가 붙은 글을 찾는다.
     * 다듬은 뒤 빈 값은 무시하고, 10개를 넘으면 400으로 막는다. (서브쿼리 IN 목록이 끝없이 길어지지 않게)
     */
    private List<String> searchHashtags(List<String> rawHashtags) {
        List<String> tags = PostPolicy.normalizeHashtags(rawHashtags).stream()
                .filter(tag -> !tag.isEmpty())
                .toList();

        if (tags.size() > PostPolicy.HASHTAG_MAX_COUNT) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return tags;
    }

    /**
     * 게시글 수정 (명세 FN-PST-003)
     *
     * 작성자 본인만 수정할 수 있다.
     * 보낸 필드만 바꾸고, 안 보낸(null) 필드는 그대로 둔다. (P5)
     * 바뀐 값이 없어도 수정 일시는 갱신된다. (P6)
     *
     * @throws BusinessException 글이 없으면 POST_NOT_FOUND(404), 남의 글이면 POST_NOT_OWNER(403)
     */
    @Transactional
    public PostResponse update(Long userId, Long postId, PostUpdateRequest request) {
        Post post = findMyPost(userId, postId);

        // 태그를 안 보냈으면(null) 다듬지 않고 null 그대로 넘긴다 → 기존 태그 유지
        List<String> hashtags = request.hashtags() == null ? null : refineHashtags(request.hashtags());
        post.update(request.title(), request.content(), request.urls(), hashtags);

        // save()를 부르지 않는 이유:
        //   이미 DB에서 꺼내 온 엔티티라 JPA가 값 변화를 지켜보고 있다가,
        //   바뀐 부분만 UPDATE 문으로 만들어 실행한다(변경 감지).
        //
        // flush()를 부르는 이유:
        //   수정 시각(updated_at)은 UPDATE가 실제로 나갈 때 채워진다.
        //   기본적으로 그 시점이 메서드가 끝난 뒤라서, 그냥 두면 응답의 updatedAt이 null로 나간다.
        //   flush()로 "지금 DB에 반영해"라고 시켜서 수정 시각이 채워진 뒤 응답을 만든다.
        postRepository.flush();

        return PostResponse.fromEntity(post, userId);
    }

    /**
     * 게시글 삭제 (명세 FN-PST-004)
     *
     * 작성자 본인만 삭제할 수 있다.
     * D-07 확정: 하드 삭제 → DB에서 실제로 지운다. 되돌릴 수 없다.
     * 글에 딸린 URL·해시태그도 함께 지워진다.
     */
    @Transactional
    public void delete(Long userId, Long postId) {
        Post post = findMyPost(userId, postId);
        postRepository.delete(post);
    }

    /**
     * 글을 찾고, 내 글이 맞는지 확인한다. (수정·삭제에서 공통으로 쓰는 부분)
     *
     * 없는 글과 남의 글을 다른 에러로 구분하는 이유:
     *   사용자 입장에서 "글이 사라졌다"와 "권한이 없다"는 다른 상황이라 안내 문구가 달라야 한다.
     *
     * 404 검사(없는 글, 탈퇴 작성자)를 403 검사보다 먼저 한다. (T14)
     */
    private Post findMyPost(Long userId, Long postId) {
        Post post = findVisiblePost(postId);

        if (!post.isOwner(userId)) {
            throw new BusinessException(ErrorCode.POST_NOT_OWNER);
        }
        return post;
    }

    /**
     * 화면에 보여도 되는 글을 작성자와 함께 찾는다. (상세·수정·삭제 공통)
     *
     * 작성자가 탈퇴한 글은 없는 글처럼 404로 숨긴다. (P8)
     * 탈퇴 후 30일 안에 복구하면 다시 보인다. 글 자체는 지우지 않았기 때문.
     */
    private Post findVisiblePost(Long postId) {
        // findWithUserById는 "있을 수도, 없을 수도 있는 결과"인 Optional로 돌려준다.
        // orElseThrow = 값이 있으면 꺼내고, 없으면 준비한 예외를 던진다.
        Post post = postRepository.findWithUserById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (post.getUser().isWithdrawn()) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }
        return post;
    }

    /**
     * 해시태그 다듬기 (P3, P4)
     *
     * 화면(프론트)에서도 '#' 제거·중복 제거를 하지만, 서버로는 어떤 값이든 들어올 수 있으므로
     * 저장 직전에 한 번 더 정리한다. (프론트를 거치지 않는 요청도 가능하기 때문)
     * 다듬는 규칙은 검색(T15)과 같아야 해서 PostPolicy에 두고 같이 쓴다.
     *
     *   입력:  ["#Spring", "spring", " JWT "]
     *   결과:  ["spring", "jwt"]
     *
     * 순서가 곧 정책이다.
     *   ① 공백·'#' 제거, 소문자 → ② 중복 제거 → ③ 10개 초과면 HASHTAG_LIMIT_EXCEEDED
     *   → ④ 1~20자 한글·영문·숫자·_ 가 아니면 INVALID_INPUT ("#"만 보낸 경우처럼 빈 값 포함)
     */
    private List<String> refineHashtags(List<String> rawHashtags) {
        List<String> refined = PostPolicy.normalizeHashtags(rawHashtags);

        if (refined.size() > PostPolicy.HASHTAG_MAX_COUNT) {
            throw new BusinessException(ErrorCode.HASHTAG_LIMIT_EXCEEDED);
        }
        if (!refined.stream().allMatch(PostPolicy::isValidHashtag)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return refined;
    }
}
