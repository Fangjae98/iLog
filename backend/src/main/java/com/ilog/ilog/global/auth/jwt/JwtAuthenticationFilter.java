package com.ilog.ilog.global.auth.jwt;

import com.ilog.ilog.global.auth.ActiveUserChecker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authorization: Bearer {accessToken} 이 유효하면 LoginUser 를 principal 로 SecurityContext 에 넣는다.
 * 이후 LoginUserArgumentResolver 가 이 principal 을 @Login 파라미터로 주입한다.
 *
 * 토큰이 없거나 무효해도 여기서 응답을 끊지 않고 그대로 통과시킨다.
 * 프론트는 모든 요청에 토큰을 붙이므로, 만료된 토큰 때문에 로그인·회원가입 같은
 * 공개 API 까지 막히면 안 된다. 로그인이 필요한 API 는 LoginUserArgumentResolver 가 401 로 막는다.
 *
 * 탈퇴한 회원의 토큰은 인증으로 쳐주지 않는다 (A6). 탈퇴해도 발급된 토큰은 만료까지
 * 살아 있는데 토큰 저장소가 없어 폐기할 수 없으므로, 요청마다 회원 상태를 본다.
 * 리졸버가 아니라 여기서 확인하는 이유는 @Login 을 선언하지 않은 인증 API 까지
 * 빠짐없이 덮기 위해서다.
 *
 * 빈으로 등록하지 않는다. @Component 로 두면 서블릿 필터로 한 번 더 자동 등록되고,
 * @WebMvcTest 슬라이스에도 딸려 들어간다. SecurityConfig 가 직접 생성해 체인에 넣는다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final ActiveUserChecker activeUserChecker;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider, ActiveUserChecker activeUserChecker) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.activeUserChecker = activeUserChecker;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.startsWithIgnoreCase(header, BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            jwtTokenProvider.parse(token)
                    // 탈퇴 회원이면 인증을 채우지 않는다. 여기서 예외를 던지면 안 된다 —
                    // 이 필터는 ExceptionTranslationFilter 보다 앞이고 @RestControllerAdvice 밖이라
                    // 톰캣 HTML 오류 페이지가 나간다. 비워 두면 EntryPoint 가 401 JSON 을 만든다.
                    .filter(loginUser -> activeUserChecker.isActive(loginUser.userId()))
                    .ifPresent(loginUser -> {
                        SecurityContext context = SecurityContextHolder.createEmptyContext();
                        context.setAuthentication(
                                UsernamePasswordAuthenticationToken.authenticated(loginUser, null, List.of()));
                        SecurityContextHolder.setContext(context);
                    });
        }

        chain.doFilter(request, response);
    }
}
