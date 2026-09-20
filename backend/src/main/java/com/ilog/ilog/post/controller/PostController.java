package com.ilog.ilog.post.controller;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginMember;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostPageResponse;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.dto.PostUpdateRequest;
import com.ilog.ilog.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/*
 * ===========================================================================
 * TODO [Member → User 이름 통일] post 패키지에서 이 파일만 옛 이름을 쓰고 있다.
 *
 * 팀에서 global의 Member를 User로 통일했지만(feat/be/user-revise 브랜치),
 * 아직 be 브랜치에 합쳐지지 않아 내 작업 공간에는 LoginUser 클래스가 없다.
 * 지금 바꾸면 컴파일이 안 되므로, 합쳐진 뒤 아래 3가지를 한 번에 바꾼다.
 *
 *   1) import  : LoginMember → LoginUser
 *   2) 파라미터 : LoginMember loginMember → LoginUser loginUser   (5곳)
 *   3) 호출     : loginMember.memberId() → loginUser.userId()     (4곳)
 *
 * 개발용 헤더 X-Member-Id → X-User-Id 는 팀원 코드에서 바뀐다.
 * post 패키지의 나머지 코드는 이미 userId / user_id 로 바꿔 두었다.
 * ===========================================================================
 *
 * [게시글 작성 흐름] ② Controller  ← 지금 이 파일 (요청이 가장 먼저 도착하는 곳)
 *
 *   ① Client ──POST /api/v1/posts──→ ② Controller → ③ Request DTO → ④ Service → ⑤ Entity → ⑥ Repository → DB
 *   ⑧ Client ←──201 + JSON────────── ② Controller ← ⑦ Response DTO ← ④ Service
 *
 * Controller = 식당의 "주문 받는 직원".
 *   - 어떤 URL로 들어온 요청을 받을지 정하고
 *   - 요청 JSON을 DTO로 받아서 Service(주방)에 넘기고
 *   - Service가 돌려준 결과를 HTTP 응답으로 내보낸다.
 *   요리(비즈니스 로직)는 하지 않는다 → Service의 일.
 *
 * 요청 예시 (개발 단계에서는 로그인 대신 헤더로 회원 번호를 보낸다)
 *
 *   POST /api/v1/posts
 *   X-Member-Id: 1                  ← 팀원 변경 반영 후 X-User-Id
 *   Content-Type: application/json
 *
 *   {
 *     "title": "첫 글",
 *     "content": "안녕하세요",
 *     "urls": ["https://a.com"],      // 선택, 여러 개 가능
 *     "hashtags": ["여행", "맛집"]     // 선택, 최대 10개
 *   }
 */
@RestController                  // 반환값을 JSON으로 바꿔서 응답하는 컨트롤러
@RequestMapping("/api/v1/posts") // 이 클래스의 모든 API 주소 앞에 붙는 공통 경로
@RequiredArgsConstructor         // final 필드(PostService)를 Spring이 넣어 줌
public class PostController {

    private final PostService postService;

    /**
     * 게시글 작성 API (명세 FN-PST-001)
     *
     * 파라미터 설명
     *   @Login LoginMember loginMember
     *     → 로그인한 회원 정보. global/auth에서 만들어 둔 기능이 자동으로 넣어 준다.
     *       로그인 정보가 없으면 여기까지 오지 않고 401(UNAUTHORIZED) 에러가 난다.
     *
     *   @Valid @RequestBody PostCreateRequest request
     *     → @RequestBody : 요청 본문(JSON)을 PostCreateRequest 객체로 변환
     *     → @Valid       : DTO에 붙인 @NotBlank, @Size 규칙 검사.
     *                      어기면 이 메서드는 실행되지 않고 400(INVALID_INPUT) 에러가 난다.
     *
     * 성공 응답: 201 Created + 저장된 게시글 JSON
     */
    @PostMapping   // HTTP POST 요청 + 주소 /api/v1/posts 를 이 메서드가 처리
    public ResponseEntity<PostResponse> create(@Login LoginMember loginMember,
                                               @Valid @RequestBody PostCreateRequest request) {

        // TODO 팀원 변경 반영 후: loginUser.userId()
        PostResponse response = postService.create(loginMember.memberId(), request);

        // ResponseEntity = 응답 상태코드 + 본문을 함께 담는 상자.
        // 새로 "만들었다"는 의미로 200(OK) 대신 201(CREATED)을 쓴다.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 게시글 조회 API (명세 FN-PST-006)
     *
     *   GET /api/v1/posts?author=me          ← 첫 쪽 (최신 글 5개)
     *   GET /api/v1/posts?author=me&page=1   ← 두 번째 쪽
     *
     * 주소가 목록·검색 API(A 담당)와 같은 /api/v1/posts라서, params로 구분한다.
     *   params = "author=me"  → author=me가 붙은 요청만 이 메서드가 처리
     *   A가 만들 일반 목록(GET /api/v1/posts)은 조건이 없으므로 서로 충돌하지 않는다.
     *
     * @RequestParam(defaultValue = "0") → page를 안 보내면 0쪽(첫 쪽)으로 본다
     *
     * 누구 글을 보여 줄지는 로그인 정보에서 정한다.
     * (회원 번호를 요청으로 받으면 남의 글 목록을 볼 수 있게 되므로)
     */
    @GetMapping(params = "author=me")
    public ResponseEntity<PostPageResponse> getMyPosts(@Login LoginMember loginMember,
                                                       @RequestParam(defaultValue = "0") int page) {
        // TODO 팀원 변경 반영 후: loginUser.userId()
        return ResponseEntity.ok(postService.getMyPosts(loginMember.memberId(), page));
    }

    /**
     * 게시글 상세 조회 API (명세 FN-PST-002)
     *
     *   GET /api/v1/posts/3      ← 3번 글 보기
     *
     * @PathVariable = 주소 안에 들어 있는 값을 꺼내는 어노테이션.
     *   @GetMapping("/{postId}")의 {postId} 자리에 들어온 3이 파라미터 postId에 담긴다.
     *   숫자가 아닌 값(/posts/abc)이 오면 400 에러가 난다. (global에서 처리)
     *
     * 성공 응답: 200 OK + 게시글 JSON
     * 없는 글 번호면: 404 POST_NOT_FOUND (Service가 예외를 던지고 global이 응답으로 변환)
     * 로그인 안 했으면: 401 UNAUTHORIZED
     *
     * 로그인한 회원만 볼 수 있다. (명세서 FN-PST-002 게시글 읽기 - 액터: 회원)
     * @Login 파라미터를 적어 두기만 하면 로그인 검사가 되고, 로그인 정보가 없으면 401로 막힌다.
     * 지금은 "누가 보는지"를 쓸 일이 없어서 loginMember 값을 사용하지는 않는다.
     * (나중에 '내 글인지 표시' 같은 기능이 생기면 여기서 쓰면 된다)
     */
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@Login LoginMember loginMember,
                                                @PathVariable Long postId) {
        // 조회는 "잘 가져왔다"는 뜻의 200 OK. ResponseEntity.ok(...)가 그 줄임 표현이다.
        return ResponseEntity.ok(postService.getPost(postId));
    }

    /**
     * 게시글 수정 API (명세 FN-PST-003)
     *
     *   PUT /api/v1/posts/3
     *
     * PUT을 쓴 이유: 보낸 내용으로 글을 통째로 바꾸기 때문. (일부만 바꾸는 방식은 보통 PATCH)
     * TODO API 명세서에 PATCH로 되어 있으면 @PutMapping → @PatchMapping 으로 바꾸면 된다.
     *
     * 작성자 본인만 가능: 남의 글이면 403 POST_NOT_OWNER
     * 없는 글이면: 404 POST_NOT_FOUND
     *
     * 성공 응답: 200 OK + 수정된 게시글 JSON
     */
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(@Login LoginMember loginMember,
                                               @PathVariable Long postId,
                                               @Valid @RequestBody PostUpdateRequest request) {
        // TODO 팀원 변경 반영 후: loginUser.userId()
        return ResponseEntity.ok(postService.update(loginMember.memberId(), postId, request));
    }

    /**
     * 게시글 삭제 API (명세 FN-PST-004)
     *
     *   DELETE /api/v1/posts/3
     *
     * 작성자 본인만 가능. D-07 확정에 따라 DB에서 실제로 지운다(복구 불가).
     *
     * 성공 응답: 204 No Content
     *   = "잘 처리했고, 돌려줄 내용은 없다"는 뜻. 지워진 글을 응답에 담을 이유가 없어서 204를 쓴다.
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@Login LoginMember loginMember,
                                       @PathVariable Long postId) {
        // TODO 팀원 변경 반영 후: loginUser.userId()
        postService.delete(loginMember.memberId(), postId);
        return ResponseEntity.noContent().build();
    }
}
