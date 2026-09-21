package com.ilog.ilog.global.config;

import com.ilog.ilog.global.auth.Login;
import com.ilog.ilog.global.auth.LoginUser;
import com.ilog.ilog.global.auth.LoginUserArgumentResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/** Swagger UI (springdoc). 접속: /swagger-ui.html, 명세: /v3/api-docs */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    static {
        // @Login LoginUser 는 서버가 채워주는 값이라 요청 파라미터로 문서화하지 않는다.
        // 다른 도메인 컨트롤러에서 @Login 을 써도 자동으로 숨겨지도록 전역으로 등록한다.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(LoginUser.class);
    }

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("iLog API")
                        .version("v1")
                        .description("""
                                매일 공부한 내용을 기록하고 공유하는 게시판 서비스 (1일 1log).

                                - 성공 응답은 래퍼 없이 데이터를 바로 반환하고, 성공·실패는 HTTP 상태 코드로 구분한다.
                                - 모든 4xx·5xx 는 공통 에러 형식(`ErrorResponse`)으로 응답한다. 프론트는 `code` 로 분기한다.
                                - 로그인이 필요한 API 에는 자물쇠 표시가 붙는다.
                                """))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("로그인(`POST /api/v1/auth/tokens`)으로 받은 accessToken. `Authorization: Bearer {accessToken}`")));
    }

    /**
     * 로그인이 필요한 API(= @Login 파라미터가 있는 메서드)에 인증 정보를 표시한다.
     * JWT 적용 전(2단계)에는 개발용 헤더로도 호출할 수 있어서 헤더 입력칸을 함께 보여준다.
     */
    @Bean
    public OperationCustomizer loginOperationCustomizer(
            @Value("${ilog.auth.dev-header-enabled:false}") boolean devHeaderEnabled) {
        return (operation, handlerMethod) -> {
            boolean loginRequired = Arrays.stream(handlerMethod.getMethodParameters())
                    .anyMatch(parameter -> parameter.hasParameterAnnotation(Login.class));
            if (!loginRequired) {
                return operation;
            }
            operation.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
            if (devHeaderEnabled) {
                operation.addParametersItem(new HeaderParameter()
                        .name(LoginUserArgumentResolver.DEV_HEADER)
                        .description("[개발용] JWT 적용 전에는 이 헤더의 회원 ID 로 로그인한 것으로 처리한다. "
                                + "JWT 적용 후 ilog.auth.dev-header-enabled=false 로 끄면 사라진다.")
                        .required(false)
                        .schema(new IntegerSchema().format("int64").example(1)));
            }
            return operation;
        };
    }
}
