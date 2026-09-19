package com.ilog.ilog.global.error;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginMember;
import com.ilog.ilog.global.config.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** comm.md 8장 "동작 확인" 항목: 에러 응답 형식과 @Login 주입 */
@WebMvcTest
@Import({SecurityConfig.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 잘못된_JSON이면_400_INVALID_INPUT() throws Exception {
        mockMvc.perform(post("/api/v1/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.timestamp").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")));
    }

    @Test
    void Valid_실패면_400과_필드별_사유() throws Exception {
        mockMvc.perform(post("/api/v1/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].reason").value("이름은 필수입니다."));
    }

    @Test
    void RequestParam_검증_실패면_400과_파라미터명() throws Exception {
        mockMvc.perform(get("/api/v1/test/param").param("email", "not-email"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].reason").value("이메일 형식이 아닙니다."));
    }

    @Test
    void 필수_파라미터_누락이면_400() throws Exception {
        mockMvc.perform(get("/api/v1/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].reason").value("필수 값입니다."));
    }

    @Test
    void PathVariable_타입_불일치면_400() throws Exception {
        mockMvc.perform(get("/api/v1/test/items/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void 없는_URL이면_404_API_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/v1/not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("API_NOT_FOUND"));
    }

    @Test
    void 지원하지_않는_Method면_405_METHOD_NOT_ALLOWED() throws Exception {
        mockMvc.perform(get("/api/v1/test/body"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void BusinessException은_ErrorCode의_상태로_변환() throws Exception {
        mockMvc.perform(get("/api/v1/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("게시글을 찾을 수 없습니다."));
    }

    @Test
    void 예상하지_못한_예외는_500() throws Exception {
        mockMvc.perform(get("/api/v1/test/unknown"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"));
    }

    @Test
    void X_Member_Id_헤더_없이_Login_API면_401() throws Exception {
        mockMvc.perform(get("/api/v1/test/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void X_Member_Id가_숫자가_아니면_401() throws Exception {
        mockMvc.perform(get("/api/v1/test/me").header("X-Member-Id", "abc"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void X_Member_Id_헤더가_있으면_LoginMember_주입() throws Exception {
        mockMvc.perform(get("/api/v1/test/me").header("X-Member-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1))
                .andExpect(jsonPath("$.tempPassword").value(false));
    }

    record TestRequest(@NotBlank(message = "이름은 필수입니다.") String name) {}

    @RestController
    static class TestController {

        @PostMapping("/api/v1/test/body")
        TestRequest body(@Valid @RequestBody TestRequest request) {
            return request;
        }

        @GetMapping("/api/v1/test/param")
        String param(@RequestParam @Email(message = "이메일 형식이 아닙니다.") String email) {
            return email;
        }

        @GetMapping("/api/v1/test/items/{id}")
        Long item(@PathVariable Long id) {
            return id;
        }

        @GetMapping("/api/v1/test/business")
        void business() {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        @GetMapping("/api/v1/test/unknown")
        void unknown() {
            throw new IllegalStateException("boom");
        }

        @GetMapping("/api/v1/test/me")
        LoginMember me(@Login LoginMember loginMember) {
            return loginMember;
        }
    }
}
