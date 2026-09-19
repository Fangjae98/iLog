package com.ilog.ilog.post.controller;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginMember;
import com.ilog.ilog.post.dto.PostCreateRequest;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
 * 요청 예시 (개발 단계에서는 로그인 대신 X-Member-Id 헤더로 회원 번호를 보낸다)
 *
 *   POST /api/v1/posts
 *   X-Member-Id: 1
 *   Content-Type: application/json
 *
 *   { "title": "첫 글", "content": "안녕하세요" }
 */
@RestController                  // 반환값을 JSON으로 바꿔서 응답하는 컨트롤러
@RequestMapping("/api/v1/posts") // 이 클래스의 모든 API 주소 앞에 붙는 공통 경로
@RequiredArgsConstructor         // final 필드(PostService)를 Spring이 넣어 줌
public class PostController {

    private final PostService postService;

    /**
     * 게시글 작성 API
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

        PostResponse response = postService.create(loginMember.memberId(), request);

        // ResponseEntity = 응답 상태코드 + 본문을 함께 담는 상자.
        // 새로 "만들었다"는 의미로 200(OK) 대신 201(CREATED)을 쓴다.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
