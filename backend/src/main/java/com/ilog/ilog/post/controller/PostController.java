package com.ilog.ilog.post.controller;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.global.error.ErrorResponse;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostPageResponse;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.dto.PostUpdateRequest;
import com.ilog.ilog.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 *   X-User-Id: 1
 *   Content-Type: application/json
 *
 *   {
 *     "title": "첫 글",
 *     "content": "안녕하세요",
 *     "urls": ["https://a.com"],      // 선택, 여러 개 가능
 *     "hashtags": ["여행", "맛집"]     // 선택, 최대 10개
 *   }
 */
@Tag(name = "Post", description = "게시글")
@RestController                  // 반환값을 JSON으로 바꿔서 응답하는 컨트롤러
@RequestMapping("/api/v1/posts") // 이 클래스의 모든 API 주소 앞에 붙는 공통 경로
@RequiredArgsConstructor         // final 필드(PostService)를 Spring이 넣어 줌
public class PostController {

    private final PostService postService;

    /**
     * 게시글 작성 API (명세 FN-PST-001)
     *
     * 파라미터 설명
     *   @Login LoginUser loginUser
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
    @Operation(summary = "게시글 작성 (FN-PST-001)",
            description = """
                    로그인한 회원이 새 게시글을 쓴다. 성공하면 201 과 저장된 게시글을 돌려준다.

                    작성자는 요청 본문이 아니라 로그인 정보에서 정한다. `urls` 와 `hashtags` 는 보내지 않아도 된다.

                    처리 순서: 입력값 검증 → 해시태그 다듬기(맨 앞 `#` 제거, 앞뒤 공백 제거, 중복 제거) → 개수 검사 → 저장.""")
    @ApiResponse(responseCode = "201", description = "작성 성공. 저장된 게시글을 돌려준다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT`(제목·내용 누락, 제목 100자 초과, `urls`·`hashtags` 의 항목이 빈 값) 또는 `HASHTAG_LIMIT_EXCEEDED`(다듬은 뒤 해시태그가 10개 초과)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping   // HTTP POST 요청 + 주소 /api/v1/posts 를 이 메서드가 처리
    public ResponseEntity<PostResponse> create(@Login LoginUser loginUser,
                                               @Valid @RequestBody PostCreateRequest request) {

        PostResponse response = postService.create(loginUser.userId(), request);

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
    @Operation(summary = "내 게시글 조회 (FN-PST-006)",
            description = """
                    로그인한 회원이 쓴 글만 최신순으로 한 쪽에 5개씩 돌려준다.

                    목록·검색 API 와 주소가 같아서 `author=me` 쿼리로 구분한다. 이 값이 없으면 이 메서드로 오지 않는다.
                    누구 글을 보여 줄지는 로그인 정보로 정하므로 회원 번호를 요청으로 받지 않는다.
                    `page` 는 0부터 시작하고, 보내지 않으면 0쪽으로 본다.
                    작성자 닉네임은 `User` 엔티티와 연결하기 전까지 null 로 나간다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공. 이번 쪽의 글 목록과 쪽 정보를 돌려준다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — `page` 가 숫자가 아니거나 음수다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    // springdoc 은 params = "author=me" 조건을 문서에 옮기지 않는다.
    // 그대로 두면 Swagger UI 의 Try it out 이 author 없이 요청을 보내 이 메서드를 못 찾는다.
    // 아래 @Parameter 는 메서드 인자에 바인딩되지 않은 문서 전용 선언이라 동작에는 영향이 없다.
    @Parameter(name = "author", in = ParameterIn.QUERY, required = true,
            description = "이 API 를 고르는 구분값. `me` 만 쓸 수 있다. 목록·검색 API 와 주소가 같아서 필요하다.",
            example = "me")
    @GetMapping(params = "author=me")
    public ResponseEntity<PostPageResponse> getMyPosts(@Login LoginUser loginUser,
                                                       @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(postService.getMyPosts(loginUser.userId(), page));
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
     * 지금은 "누가 보는지"를 쓸 일이 없어서 loginUser 값을 사용하지는 않는다.
     * (나중에 '내 글인지 표시' 같은 기능이 생기면 여기서 쓰면 된다)
     */
    @Operation(summary = "게시글 상세 조회 (FN-PST-002)",
            description = """
                    게시글 하나를 제목·본문·URL·해시태그까지 모두 돌려준다. 로그인한 회원만 볼 수 있다.
                    지금은 누가 보는지를 쓰지 않아서 남의 글도 볼 수 있다.""")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — `postId` 에 숫자가 아닌 값을 보냈다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "`POST_NOT_FOUND` — 그 번호의 글이 없다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@Login LoginUser loginUser,
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
    @Operation(summary = "게시글 수정 (FN-PST-003)",
            description = """
                    작성자 본인만 수정할 수 있다. 보낸 내용으로 제목·본문·URL·해시태그를 통째로 교체한다(D-06).

                    `urls` 나 `hashtags` 를 보내지 않거나 빈 배열로 보내면 원래 있던 값이 모두 지워진다.
                    없는 글(404)과 남의 글(403)은 다른 코드로 구분해서 돌려준다.""")
    @ApiResponse(responseCode = "200", description = "수정 성공. 수정 일시가 채워진 게시글을 돌려준다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT`(제목·내용 누락, 제목 100자 초과, 항목이 빈 값, `postId` 가 숫자 아님) 또는 `HASHTAG_LIMIT_EXCEEDED`(다듬은 뒤 해시태그가 10개 초과)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "`POST_NOT_OWNER` — 남이 쓴 글이다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "`POST_NOT_FOUND` — 그 번호의 글이 없다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> update(@Login LoginUser loginUser,
                                               @PathVariable Long postId,
                                               @Valid @RequestBody PostUpdateRequest request) {
        return ResponseEntity.ok(postService.update(loginUser.userId(), postId, request));
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
    @Operation(summary = "게시글 삭제 (FN-PST-004)",
            description = """
                    작성자 본인만 삭제할 수 있다. D-07 확정에 따라 DB 에서 실제로 지우므로 되돌릴 수 없다.
                    글에 딸린 URL·해시태그도 함께 지워진다. 돌려줄 내용이 없어서 204 로 응답한다.""")
    @ApiResponse(responseCode = "204", description = "삭제 성공. 본문 없음")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — `postId` 에 숫자가 아닌 값을 보냈다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "`POST_NOT_OWNER` — 남이 쓴 글이다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "`POST_NOT_FOUND` — 그 번호의 글이 없다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@Login LoginUser loginUser,
                                       @PathVariable Long postId) {
        postService.delete(loginUser.userId(), postId);
        return ResponseEntity.noContent().build();
    }
}
