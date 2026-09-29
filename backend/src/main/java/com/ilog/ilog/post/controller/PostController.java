package com.ilog.ilog.post.controller;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.global.error.ErrorResponse;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostCreateResponse;
import com.ilog.ilog.post.dto.PostPageResponse;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.dto.PostSearchRequest;
import com.ilog.ilog.post.dto.PostUpdateRequest;
import com.ilog.ilog.post.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/*
 * [게시글 작성 흐름] ② Controller  ← 지금 이 파일 (요청이 가장 먼저 도착하는 곳)
 *
 *   ① Client ──POST /api/v1/posts──→ ② Controller → ③ Request DTO → ④ Service → ⑤ Entity → ⑥ Repository → DB
 *   ⑧ Client ←──201 + {postId}────── ② Controller ← ⑦ Response DTO ← ④ Service
 *
 * Controller = 식당의 "주문 받는 직원".
 *   - 어떤 URL로 들어온 요청을 받을지 정하고
 *   - 요청 JSON을 DTO로 받아서 Service(주방)에 넘기고
 *   - Service가 돌려준 결과를 HTTP 응답으로 내보낸다.
 *   요리(비즈니스 로직)는 하지 않는다 → Service의 일.
 *
 * 요청 예시 (로그인해서 받은 accessToken 을 Authorization 헤더에 붙인다)
 *
 *   POST /api/v1/posts
 *   Authorization: Bearer {accessToken}
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
     * 성공 응답: 201 Created + { "postId": 새 글 번호 } + Location 헤더
     */
    @Operation(summary = "게시글 작성 (FN-PST-001)",
            description = """
                    로그인한 회원이 새 게시글을 쓴다. 성공하면 201 과 새 글 번호(`postId`)를 돌려준다(P7).

                    작성자는 요청 본문이 아니라 로그인 정보에서 정한다. 제목·내용은 필수, `urls` 와 `hashtags` 는 보내지 않아도 된다(P1).

                    처리 순서: 입력값 검증(제목 100자·내용 1000자·URL 5개) → 해시태그 다듬기(앞뒤 공백·맨 앞 `#` 제거, 소문자, 중복 제거)
                    → 개수 검사(10개) → 글자 규칙 검사(1~20자 한글·영문·숫자·`_`) → 저장.""")
    @ApiResponse(responseCode = "201", description = "작성 성공. 새 글 번호를 돌려주고, `Location` 헤더에 새 글 주소(`/api/v1/posts/{postId}`)를 담는다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT`(제목·내용 누락, 제목 100자·내용 1000자 초과, URL 6개 이상·형식·2,048자 초과, 해시태그 글자 규칙 위반) "
            + "또는 `HASHTAG_LIMIT_EXCEEDED`(다듬은 뒤 해시태그가 10개 초과)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PostMapping   // HTTP POST 요청 + 주소 /api/v1/posts 를 이 메서드가 처리
    public ResponseEntity<PostCreateResponse> create(@Login LoginUser loginUser,
                                                     @Valid @RequestBody PostCreateRequest request) {

        PostCreateResponse response = postService.create(loginUser.userId(), request);

        // ResponseEntity = 응답 상태코드 + 헤더 + 본문을 함께 담는 상자.
        // created(...) = 새로 "만들었다"는 201(CREATED) + 새 글 주소를 담은 Location 헤더 (P7)
        return ResponseEntity.created(URI.create("/api/v1/posts/" + response.postId())).body(response);
    }

    /**
     * 게시글 목록·검색·내 글 API (PST-02, FN-PST-006)
     *
     *   GET /api/v1/posts                              ← 전체 목록 첫 쪽 (최신 글 10개)
     *   GET /api/v1/posts?keyword=jpa&hashtag=spring   ← 조건 검색 (모두 만족하는 글)
     *   GET /api/v1/posts?author=me&page=1             ← 내 글 두 번째 쪽
     *
     * 목록·검색·내 글을 핸들러 하나로 합쳤다. (S1)
     * 같은 주소에 핸들러가 둘이면 springdoc이 문서에서 하나로 합쳐 버려 Swagger가 깨지기 때문이다.
     *
     * @ParameterObject + @ModelAttribute : 쿼리 파라미터 여러 개를 PostSearchRequest 하나로 받는다.
     *   Swagger에는 record의 필드가 쿼리 파라미터 하나하나로 나온다.
     *
     * 누구 글인지(author=me)는 로그인 정보에서 정한다.
     * (회원 번호를 요청으로 받으면 남의 글 목록을 "내 글"처럼 볼 수 있게 되므로)
     */
    @Operation(summary = "게시글 목록·검색·내 글 (PST-02, FN-PST-006)",
            description = """
                    조건에 맞는 글을 최신순(작성 시각, 같으면 번호 역순)으로 한 쪽씩 돌려준다. 정렬은 바꿀 수 없고 `sort` 는 무시한다.

                    모든 조건은 선택이고, 여러 개를 보내면 **모두 만족하는 글**만 찾는다(AND).
                    - `keyword`: 제목 또는 본문 / `title`: 제목 — 부분 일치, 대소문자 무시
                    - `nickname`: 작성자 닉네임 정확 일치 / `author=me`: 로그인한 회원의 글
                    - `hashtag`: 반복해서 보내면 그 태그가 **모두** 붙은 글. 저장과 같이 `#`·공백 제거, 소문자로 비교한다
                    - `date`: 그날(KST) 작성된 글

                    작성자가 탈퇴한 글은 나오지 않는다(P8). `page` 는 0부터, `size` 는 기본 10·최대 50.""")
    @ApiResponse(responseCode = "200", description = "조회 성공. 결과가 없으면 `content` 가 빈 배열이다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — `page` 음수, `size` 가 1~50 밖, `date` 형식 오류, `author` 가 `me` 가 아님, 해시태그 10개 초과",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping
    public ResponseEntity<PostPageResponse> search(@Login LoginUser loginUser,
                                                   @ParameterObject @Valid @ModelAttribute PostSearchRequest request) {
        return ResponseEntity.ok(postService.search(loginUser.userId(), request));
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
                    게시글 하나를 제목·본문·URL·해시태그·작성자까지 모두 돌려준다. 로그인한 회원만 볼 수 있고, 남의 글도 볼 수 있다.
                    `isMine` 은 로그인한 회원이 쓴 글인지다. 프론트는 이 값으로 수정·삭제 버튼을 보여 준다(P7).
                    작성자가 탈퇴한 글은 없는 글과 같이 404 다(P8).""")
    @ApiResponse(responseCode = "200", description = "조회 성공")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT` — `postId` 에 숫자가 아닌 값을 보냈다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "`POST_NOT_FOUND` — 그 번호의 글이 없거나 작성자가 탈퇴했다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPost(@Login LoginUser loginUser,
                                                @PathVariable Long postId) {
        // 조회는 "잘 가져왔다"는 뜻의 200 OK. ResponseEntity.ok(...)가 그 줄임 표현이다.
        return ResponseEntity.ok(postService.getPost(loginUser.userId(), postId));
    }

    /**
     * 게시글 수정 API (명세 FN-PST-003)
     *
     *   PATCH /api/v1/posts/3
     *
     * PATCH를 쓰는 이유: 보낸 필드만 바꾸는 부분 수정이기 때문. (P5, 프론트가 PATCH로 호출)
     * 수정 화면은 네 필드를 모두 보내므로 결과는 통째로 교체와 같다.
     *
     * 작성자 본인만 가능: 남의 글이면 403 POST_NOT_OWNER
     * 없는 글이면: 404 POST_NOT_FOUND
     *
     * 성공 응답: 200 OK + 수정된 게시글 JSON
     */
    @Operation(summary = "게시글 수정 (FN-PST-003)",
            description = """
                    작성자 본인만 수정할 수 있다. **보낸 필드만 바꾼다**(P5). 보내지 않은 필드는 원래 값을 그대로 둔다.

                    `urls` 나 `hashtags` 를 보내면 그 목록으로 통째로 교체하고, 빈 배열로 보내면 모두 지운다.
                    바뀐 값이 없어도 수정 일시(`updatedAt`)는 갱신된다(P6).
                    없는 글(404)과 남의 글(403)은 다른 코드로 구분해서 돌려준다.""")
    @ApiResponse(responseCode = "200", description = "수정 성공. 수정 일시가 채워진 게시글을 돌려준다")
    @ApiResponse(responseCode = "400", description = "`INVALID_INPUT`(제목·내용을 빈 값으로 보냄, 길이·개수·URL 형식·해시태그 글자 규칙 위반, `postId` 가 숫자 아님) "
            + "또는 `HASHTAG_LIMIT_EXCEEDED`(다듬은 뒤 해시태그가 10개 초과)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "`UNAUTHORIZED` — 로그인 정보가 없거나 올바르지 않다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "`POST_NOT_OWNER` — 남이 쓴 글이다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "`POST_NOT_FOUND` — 그 번호의 글이 없거나 작성자가 탈퇴했다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @PatchMapping("/{postId}")
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
    @ApiResponse(responseCode = "404", description = "`POST_NOT_FOUND` — 그 번호의 글이 없거나 작성자가 탈퇴했다",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@Login LoginUser loginUser,
                                       @PathVariable Long postId) {
        postService.delete(loginUser.userId(), postId);
        return ResponseEntity.noContent().build();
    }
}
