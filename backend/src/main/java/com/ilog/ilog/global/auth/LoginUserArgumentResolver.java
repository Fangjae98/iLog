package com.ilog.ilog.global.auth;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String DEV_HEADER = "X-User-Id";   // Swagger 문서(OpenApiConfig)도 이 값을 쓴다

    @Value("${ilog.auth.dev-header-enabled:false}")
    private boolean devHeaderEnabled;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Login.class)
                && parameter.getParameterType().equals(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        // 3단계: JWT 필터가 SecurityContext에 LoginUser를 principal로 넣어둔다
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }

        // 2단계 전용: 개발용 헤더
        if (devHeaderEnabled) {
            String header = webRequest.getHeader(DEV_HEADER);
            if (header != null) {
                try {
                    return new LoginUser(Long.parseLong(header), false);
                } catch (NumberFormatException e) {   // 숫자가 아니면 500 대신 401
                    throw new BusinessException(ErrorCode.UNAUTHORIZED);
                }
            }
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
