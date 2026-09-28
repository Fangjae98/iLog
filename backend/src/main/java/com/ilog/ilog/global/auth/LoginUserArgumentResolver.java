package com.ilog.ilog.global.auth;

import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * {@code @Login LoginUser} 파라미터에 로그인 회원을 주입한다.
 *
 * <p>인증 주체는 {@code JwtAuthenticationFilter} 가 SecurityContext 에 넣어 둔 principal 하나뿐이다.
 * 개발용 {@code X-User-Id} 헤더는 T08 에서 제거했다 — 헤더에 숫자만 넣으면 아무나 될 수 있었다.
 *
 * <p>SecurityConfig 가 이미 인증을 요구하므로 여기까지 왔다면 대개 principal 이 있다.
 * 그래도 공개 경로에 실수로 {@code @Login} 을 붙인 경우를 대비해 401 을 남겨 둔다.
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(Login.class)
                && parameter.getParameterType().equals(LoginUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
}
