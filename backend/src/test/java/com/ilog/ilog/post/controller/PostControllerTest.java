package com.ilog.ilog.post.controller;

import com.ilog.ilog.global.config.SecurityConfig;
import com.ilog.ilog.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 잘 만든 요청이 500 으로 나가지 않는지 확인한다.
 * 둘 다 Swagger 문서에 400 으로 적어 둔 경우라, 문서와 실제 동작이 어긋나지 않게 막아 두는 테스트다.
 */
@WebMvcTest(PostController.class)
@Import(SecurityConfig.class)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PostService postService;

    @Test
    @DisplayName("page 가 음수면 500 이 아니라 400 INVALID_INPUT")
    void page가_음수면_400() throws Exception {
        // @PositiveOrZero 가 없으면 PageRequest.of 가 IllegalArgumentException 을 던져 500 이 된다
        mockMvc.perform(get("/api/v1/posts?author=me&page=-1").header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("author 를 빠뜨리면 500 이 아니라 400 INVALID_INPUT")
    void author가_없으면_400() throws Exception {
        // params = "author=me" 조건에 안 맞으면 UnsatisfiedServletRequestParameterException 이 나는데,
        // GlobalExceptionHandler 가 받지 않으면 잘 만든 요청에도 500 이 나간다
        mockMvc.perform(get("/api/v1/posts").header("X-User-Id", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
