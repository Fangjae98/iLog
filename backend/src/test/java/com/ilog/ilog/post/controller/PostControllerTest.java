package com.ilog.ilog.post.controller;

import com.ilog.ilog.support.SecuredSliceTestSupport;
import com.ilog.ilog.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
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
    @DisplayName("author 를 빠뜨리면 500 이 아니라 400 INVALID_INPUT")
    void author가_없으면_400() throws Exception {
        // params = "author=me" 조건에 안 맞으면 UnsatisfiedServletRequestParameterException 이 나는데,
        // GlobalExceptionHandler 가 받지 않으면 잘 만든 요청에도 500 이 나간다
        mockMvc.perform(get("/api/v1/posts").header(HttpHeaders.AUTHORIZATION, bearer(1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
