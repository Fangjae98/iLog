package com.ilog.ilog.post.controller;

import com.ilog.ilog.support.SecuredSliceTestSupport;
import com.ilog.ilog.post.dto.PostAuthorResponse;
import com.ilog.ilog.post.dto.PostCreateResponse;
import com.ilog.ilog.post.dto.PostResponse;
import com.ilog.ilog.post.dto.PostSearchRequest;
import com.ilog.ilog.post.dto.PostUpdateRequest;
import com.ilog.ilog.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시글 API 의 인증과 입력 검증을 확인한다.
 *
 * 잘 만든 요청이 500 으로 나가지 않는지 확인한다.
 * 둘 다 Swagger 문서에 400 으로 적어 둔 경우라, 문서와 실제 동작이 어긋나지 않게 막아 두는 테스트다.
 */
@WebMvcTest(PostController.class)
class PostControllerTest extends SecuredSliceTestSupport {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PostService postService;

    @Test
    @DisplayName("토큰 없이 게시글 API 를 부르면 401 JSON")
    void 토큰_없이_부르면_401_JSON() throws Exception {
        // T08 완료 기준. 톰캣 기본 HTML 오류 페이지가 아니라 공통 에러 형식이어야 한다 (지시서 3장).
        mockMvc.perform(get("/api/v1/posts?author=me"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("탈퇴한 회원의 토큰이면 401")
    void 탈퇴_회원_토큰이면_401() throws Exception {
        // A6: 탈퇴해도 토큰은 만료까지 살아 있으므로 요청마다 회원 상태를 본다
        given(activeUserChecker.isActive(1L)).willReturn(false);

        mockMvc.perform(get("/api/v1/posts?author=me").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("임시 비밀번호 토큰으로도 게시글 API 를 쓸 수 있다")
    void 임시비밀번호_토큰도_막지_않는다() throws Exception {
        // A8: 서버는 tempPassword 로 인가를 걸지 않는다. 비밀번호 변경 강제는 프론트 담당이다.
        // 이걸 막으면 비번을 바꾼 뒤에도 기존 토큰의 tmp=true 때문에 만료까지 계속 막힌다.
        mockMvc.perform(get("/api/v1/posts?author=me").header(HttpHeaders.AUTHORIZATION, bearer(1, true)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("page 가 음수면 500 이 아니라 400 INVALID_INPUT")
    void page가_음수면_400() throws Exception {
        // @PositiveOrZero 가 없으면 PageRequest.of 가 IllegalArgumentException 을 던져 500 이 된다
        mockMvc.perform(get("/api/v1/posts?author=me&page=-1").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("author 가 me 가 아니면 400 INVALID_INPUT")
    void author가_me가_아니면_400() throws Exception {
        // 목록·검색과 핸들러를 합쳐서(S1) author 는 선택 조건이 됐다. 쓸 수 있는 값은 me 하나다
        mockMvc.perform(get("/api/v1/posts?author=you").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(postService);
    }

    // ---------- T15 목록·검색 요청 (S3, S4) ----------

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"size=51", "size=0", "date=2026-13-01", "date=2026/09/16", "page=abc"})
    @DisplayName("쪽 크기·날짜·쪽 번호 형식이 틀리면 400 INVALID_INPUT")
    void 검색_파라미터_형식_오류는_400(String query) throws Exception {
        mockMvc.perform(get("/api/v1/posts?" + query).header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("조건 없이 부르면 0쪽, 10개로 조회한다")
    void 조건_없으면_기본값() throws Exception {
        mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isOk());

        verify(postService).search(1L, new PostSearchRequest(null, null, null, null, null, null, null, null));
        assertThat(new PostSearchRequest(null, null, null, null, null, null, null, null))
                .extracting(PostSearchRequest::page, PostSearchRequest::size)
                .containsExactly(0, 10);
    }

    @Test
    @DisplayName("hashtag 를 반복해서 보내면 목록으로 받고, sort 같은 모르는 파라미터는 무시한다")
    void 반복_해시태그와_sort_무시() throws Exception {
        mockMvc.perform(get("/api/v1/posts?hashtag=spring&hashtag=jpa&date=2026-09-16&sort=createdAt,asc&size=50")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isOk());

        verify(postService).search(1L, new PostSearchRequest(null, null, null, List.of("spring", "jpa"),
                LocalDate.of(2026, 9, 16), null, 0, 50));
    }

    // ---------- T13 입력 규칙 (P1, P2) ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("잘못된_작성_요청")
    @DisplayName("작성 요청이 규칙을 어기면 400 INVALID_INPUT 이고 서비스까지 가지 않는다")
    void 작성_규칙_위반은_400(String 경우, String body) throws Exception {
        mockMvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(postService);
    }

    static Stream<Arguments> 잘못된_작성_요청() {
        String urls6 = IntStream.range(0, 6).mapToObj(i -> "\"https://a.com/" + i + "\"").collect(Collectors.joining(","));
        String longUrl = "https://a.com/" + "a".repeat(2049 - "https://a.com/".length());
        return Stream.of(
                Arguments.of("content 없음", """
                        {"title":"제목"}"""),
                Arguments.of("content 1001자", "{\"title\":\"제목\",\"content\":\"" + "가".repeat(1001) + "\"}"),
                Arguments.of("content 공백만", """
                        {"title":"제목","content":"   "}"""),
                Arguments.of("urls 6개", "{\"title\":\"제목\",\"content\":\"내용\",\"urls\":[" + urls6 + "]}"),
                Arguments.of("ftp URL", """
                        {"title":"제목","content":"내용","urls":["ftp://a"]}"""),
                Arguments.of("URL 2049자", "{\"title\":\"제목\",\"content\":\"내용\",\"urls\":[\"" + longUrl + "\"]}")
        );
    }

    @Test
    @DisplayName("URL 2048자는 받는다")
    void URL_2048자는_받는다() throws Exception {
        String url = "https://a.com/" + "a".repeat(2048 - "https://a.com/".length());
        given(postService.create(eq(1L), any())).willReturn(new PostCreateResponse(1L));

        mockMvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"제목\",\"content\":\"내용\",\"urls\":[\"" + url + "\"]}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("수정에서 제목을 공백만 보내면 400 INVALID_INPUT")
    void 수정_제목_공백은_400() throws Exception {
        mockMvc.perform(patch("/api/v1/posts/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"  "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        verifyNoInteractions(postService);
    }

    @Test
    @DisplayName("수정은 PATCH 이고 보내지 않은 필드는 null 로 서비스에 넘긴다")
    void 수정은_PATCH_부분_수정() throws Exception {
        mockMvc.perform(patch("/api/v1/posts/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":" 새 제목 "}"""))
                .andExpect(status().isOk());

        verify(postService).update(1L, 1L, new PostUpdateRequest("새 제목", null, null, null));
    }

    @Test
    @DisplayName("수정을 PUT 으로 부르면 405")
    void 수정을_PUT으로_부르면_405() throws Exception {
        mockMvc.perform(put("/api/v1/posts/1")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"새 제목"}"""))
                .andExpect(status().isMethodNotAllowed());
    }

    // ---------- T14 응답 (P7) ----------

    @Test
    @DisplayName("작성하면 201, 새 글 번호, Location 헤더")
    void 작성_응답은_postId와_Location() throws Exception {
        given(postService.create(eq(1L), any())).willReturn(new PostCreateResponse(12L));

        mockMvc.perform(post("/api/v1/posts")
                        .header(HttpHeaders.AUTHORIZATION, bearer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"제목","content":"내용"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, "/api/v1/posts/12"))
                .andExpect(jsonPath("$.postId").value(12));
    }

    @Test
    @DisplayName("상세 응답은 postId, author, isMine 이름으로 나간다")
    void 상세_응답_필드_이름() throws Exception {
        given(postService.getPost(1L, 12L)).willReturn(new PostResponse(12L, "제목", "내용", List.of(), List.of("spring"),
                new PostAuthorResponse(1L, "기택"), true, LocalDateTime.of(2026, 9, 16, 10, 30, 0, 5), null));

        mockMvc.perform(get("/api/v1/posts/12").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postId").value(12))
                .andExpect(jsonPath("$.author.userId").value(1))
                .andExpect(jsonPath("$.author.nickname").value("기택"))
                .andExpect(jsonPath("$.isMine").value(true))
                .andExpect(jsonPath("$.mine").doesNotExist())
                .andExpect(jsonPath("$.createdAt").value("2026-09-16T10:30:00"))
                .andExpect(jsonPath("$.updatedAt").value((Object) null));
    }
}
