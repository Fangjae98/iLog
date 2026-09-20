package com.ilog.ilog.global.config;

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
 * 다른 도메인 컨트롤러를 문서에서 확인하려면 controllers 에 추가하고 필요한 서비스를 @MockitoBean 으로 채운다.
 */
@WebMvcTest(UserController.class)
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

    @Test
    void 문서_정보() throws Exception {
        mockMvc.perform(get(DOCS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("iLog API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"));
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
    void Swagger_UI_페이지가_열린다() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }
}
