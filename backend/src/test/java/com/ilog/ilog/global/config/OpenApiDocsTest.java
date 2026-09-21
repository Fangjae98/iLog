package com.ilog.ilog.global.config;

import com.ilog.ilog.auth.controller.AuthController;
import com.ilog.ilog.auth.service.AuthService;
import com.ilog.ilog.post.controller.PostController;
import com.ilog.ilog.post.service.PostService;
import com.ilog.ilog.user.controller.UserController;
import com.ilog.ilog.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.configuration.SpringDocSpecPropertiesConfiguration;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiOAuthProperties;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.springdoc.webmvc.ui.SwaggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Swagger 문서가 의도대로 만들어지는지 확인한다. DB 없이 돌도록 컨트롤러 슬라이스에 springdoc 만 얹었다.
 * 새 도메인 컨트롤러를 문서에서 확인하려면 아래 목록에 추가하고 필요한 서비스를 @MockitoBean 으로 채운다.
 * (Post 가 그 예다 — PostController 를 넣고 PostService 를 목으로 채웠다)
 */
@WebMvcTest({UserController.class, PostController.class, AuthController.class})
@Import({SecurityConfig.class, OpenApiConfig.class})
@ImportAutoConfiguration({
        SpringDocConfiguration.class, SpringDocConfigProperties.class, SpringDocSpecPropertiesConfiguration.class,
        SpringDocWebMvcConfiguration.class,
        SwaggerConfig.class, SwaggerUiConfigProperties.class, SwaggerUiOAuthProperties.class
})
class OpenApiDocsTest {

    private static final String DOCS = "/v3/api-docs";

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @MockitoBean
    PostService postService;

    @MockitoBean
    AuthService authService;

    @Test
    void 문서_정보() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("iLog API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
                .andExpect(jsonPath("$.tags[?(@.name=='User')].description").value("회원"))
                .andExpect(jsonPath("$.tags[?(@.name=='Post')].description").value("게시글"))
                .andExpect(jsonPath("$.tags[?(@.name=='Auth')].description").value("인증"));
    }

    @Test
    void 회원_API_5개가_문서에_나온다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/users'].post.summary").value("회원가입 (MBR-01)"))
                .andExpect(jsonPath("$.paths['/api/v1/users/email-availability'].get.summary").value("이메일 사용 가능 확인 (MBR-02)"))
                .andExpect(jsonPath("$.paths['/api/v1/users/nickname-availability'].get.summary").value("닉네임 사용 가능 확인 (MBR-03)"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me/password-verification'].post.summary").value("비밀번호 재확인 + 개인정보 조회 (MBR-05)"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.summary").value("닉네임 수정 (MBR-06)"));
    }

    @Test
    void 중복확인은_쿼리_파라미터로_문서화된다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/users/email-availability'].get.parameters[?(@.name=='email' && @.in=='query')]").isNotEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/users/nickname-availability'].get.parameters[?(@.name=='nickname' && @.in=='query')]").isNotEmpty());
    }

    @Test
    void 로그인이_필요한_API만_인증과_개발용_헤더가_붙는다() throws Exception {
        String reauth = "$.paths['/api/v1/users/me/password-verification'].post";
        String patch = "$.paths['/api/v1/users/me'].patch";
        String signup = "$.paths['/api/v1/users'].post";
        String emailCheck = "$.paths['/api/v1/users/email-availability'].get";

        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath(reauth + ".security[0].bearerAuth").exists())
                .andExpect(jsonPath(patch + ".security[0].bearerAuth").exists())
                .andExpect(jsonPath(reauth + ".parameters[?(@.name=='X-User-Id' && @.in=='header')]").isNotEmpty())
                .andExpect(jsonPath(patch + ".parameters[?(@.name=='X-User-Id' && @.in=='header')]").isNotEmpty())
                // 로그인 없이 부르는 API 에는 붙지 않는다
                .andExpect(jsonPath(signup + ".security").doesNotExist())
                .andExpect(jsonPath(emailCheck + ".security").doesNotExist())
                .andExpect(jsonPath(emailCheck + ".parameters[?(@.name=='X-User-Id')]").isEmpty());
    }

    @Test
    void LoginUser는_요청_파라미터로_새어나가지_않는다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/users/me/password-verification'].post.parameters[?(@.name=='userId' || @.name=='tempPassword')]").isEmpty())
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.parameters[?(@.name=='userId' || @.name=='tempPassword')]").isEmpty())
                .andExpect(jsonPath("$.components.schemas.LoginUser").doesNotExist());
    }

    @Test
    void 실패_응답은_공통_에러_형식을_가리킨다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/users'].post.responses['409'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/users/me'].patch.responses['404'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.code").exists());
    }

    @Test
    void 요청_스키마에_검증_규칙과_예시가_반영된다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.components.schemas.SignupRequest.required").isArray())
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.email.maxLength").value(100))
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.email.example").value("user@example.com"))
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.password.format").value("password"))
                .andExpect(jsonPath("$.components.schemas.SignupRequest.properties.nickname.pattern").exists());
    }

    @Test
    void 게시글_API_5개가_문서에_나온다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/posts'].post.summary").value("게시글 작성 (FN-PST-001)"))
                .andExpect(jsonPath("$.paths['/api/v1/posts'].get.summary").value("내 게시글 조회 (FN-PST-006)"))
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].get.summary").value("게시글 상세 조회 (FN-PST-002)"))
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].put.summary").value("게시글 수정 (FN-PST-003)"))
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].delete.summary").value("게시글 삭제 (FN-PST-004)"));
    }

    /**
     * springdoc 은 params = "author=me" 조건을 문서로 옮기지 않아서 @Parameter 로 직접 적어 두었다.
     * 이게 빠지면 Swagger UI 의 Try it out 이 author 없이 요청을 보내 500 이 난다.
     */
    @Test
    void 내_게시글_조회는_author와_page_쿼리로_문서화된다() throws Exception {
        String myPosts = "$.paths['/api/v1/posts'].get";

        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath(myPosts + ".parameters[?(@.name=='author' && @.in=='query')]").isNotEmpty())
                .andExpect(jsonPath(myPosts + ".parameters[?(@.name=='author')].required").value(true))
                .andExpect(jsonPath(myPosts + ".parameters[?(@.name=='page' && @.in=='query')]").isNotEmpty());
    }

    @Test
    void 게시글_API는_모두_로그인이_필요하다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/posts'].post.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].put.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].delete.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/v1/posts'].post.parameters[?(@.name=='X-User-Id' && @.in=='header')]").isNotEmpty())
                // LoginUser 는 전역 설정으로 숨겨져 있어서 요청 파라미터로 새어 나오지 않는다
                .andExpect(jsonPath("$.paths['/api/v1/posts'].post.parameters[?(@.name=='userId' || @.name=='tempPassword')]").isEmpty());
    }

    @Test
    void 게시글_성공_응답은_응답_DTO_스키마를_가리킨다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/posts'].post.responses['201'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/PostResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/posts'].get.responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/PostPageResponse"))
                // 삭제는 돌려줄 내용이 없어서 204 에 본문 스키마가 붙지 않는다
                .andExpect(jsonPath("$.paths['/api/v1/posts/{postId}'].delete.responses['204'].content").doesNotExist());
    }

    @Test
    void 게시글_실패_응답은_공통_에러_형식을_가리킨다() throws Exception {
        String detail = "$.paths['/api/v1/posts/{postId}']";

        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.paths['/api/v1/posts'].post.responses['400'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath(detail + ".get.responses['404'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath(detail + ".put.responses['403'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath(detail + ".delete.responses['401'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"));
    }

    @Test
    void 게시글_DTO에_설명과_예시가_반영된다() throws Exception {
        mockMvc.perform(get(DOCS))
                // @Size(max = 100) 이 스키마에 자동 반영되므로 @Schema 에 maxLength 를 적지 않는다
                .andExpect(jsonPath("$.components.schemas.PostCreateRequest.properties.title.maxLength").value(100))
                .andExpect(jsonPath("$.components.schemas.PostCreateRequest.properties.title.example").exists())
                .andExpect(jsonPath("$.components.schemas.PostCreateRequest.properties.hashtags.description").exists())
                .andExpect(jsonPath("$.components.schemas.PostCreateRequest.properties.hashtags.example[0]").value("여행"))
                .andExpect(jsonPath("$.components.schemas.PostPageResponse.properties.size.example").value(5))
                .andExpect(jsonPath("$.components.schemas.PostSummaryResponse.properties.nickname.description").exists());
    }

    @Test
    void 공통_에러_형식에_설명과_예시가_붙는다() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.status.example").value(400))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.code.example").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.components.schemas.ErrorResponse.properties.errors.description").exists())
                .andExpect(jsonPath("$.components.schemas.FieldError.properties.field.example").value("title"))
                .andExpect(jsonPath("$.components.schemas.FieldError.properties.reason.example").value("제목은 필수입니다."));
    }

    @Test
    void 로그인_API는_인증_없이_부르고_토큰_응답_스키마를_가리킨다() throws Exception {
        String login = "$.paths['/api/v1/auth/tokens'].post";

        mockMvc.perform(get(DOCS))
                .andExpect(jsonPath(login + ".summary").value("로그인 (토큰 발급)"))
                // 로그인 전에 부르는 API 라 자물쇠와 개발용 헤더가 붙지 않는다
                .andExpect(jsonPath(login + ".security").doesNotExist())
                .andExpect(jsonPath(login + ".parameters").doesNotExist())
                .andExpect(jsonPath(login + ".responses['200'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/LoginResponse"))
                .andExpect(jsonPath(login + ".responses['401'].content['application/json'].schema['$ref']")
                        .value("#/components/schemas/ErrorResponse"))
                .andExpect(jsonPath("$.components.schemas.LoginRequest.properties.password.format").value("password"))
                .andExpect(jsonPath("$.components.schemas.LoginResponse.properties.user").exists())
                // 응답 안의 회원 요약이 LoginUser 스키마로 새어 나오지 않는다
                .andExpect(jsonPath("$.components.schemas.LoginUser").doesNotExist());
    }

    @Test
    void Swagger_UI_페이지가_열린다() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
