package com.ilog.ilog.global.config;

import com.ilog.ilog.global.auth.ActiveUserChecker;
import com.ilog.ilog.global.auth.jwt.JwtAuthenticationFilter;
import com.ilog.ilog.global.auth.jwt.JwtProperties;
import com.ilog.ilog.global.auth.jwt.JwtTokenProvider;
import com.ilog.ilog.global.error.BusinessException;
import com.ilog.ilog.global.error.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            ActiveUserChecker activeUserChecker,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) throws Exception {

        http
            .csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, activeUserChecker),
                    UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(unauthorizedEntryPoint(handlerExceptionResolver))
                    .accessDeniedHandler(accessDeniedHandler(handlerExceptionResolver)))
            // 메서드까지 함께 지정해야 한다. POST /auth/tokens(로그인)는 공개지만
            // DELETE /auth/tokens(로그아웃)는 인증이 필요하다.
            .authorizeHttpRequests(a -> a
                    .requestMatchers(HttpMethod.POST,
                            "/api/v1/auth/tokens",                  // 로그인
                            "/api/v1/auth/temporary-passwords",     // 임시 비밀번호 발급
                            "/api/v1/auth/account-recoveries",      // 탈퇴 계정 복구 (T11 에서 구현)
                            "/api/v1/users").permitAll()            // 회원가입
                    .requestMatchers(HttpMethod.GET,
                            "/api/v1/users/email-availability",
                            "/api/v1/users/nickname-availability").permitAll()
                    .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                            "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                    .anyRequest().authenticated());

        // 임시 비밀번호 상태는 서버에서 막지 않는다 (A8).
        // LoginUser.tempPassword 로 인가를 걸면, 비밀번호를 바꾼 뒤에도 기존 토큰의
        // tmp=true 가 만료까지 남아 계속 막히게 된다. 변경 화면 강제는 프론트 담당이다.
        return http.build();
    }

    /**
     * 인증이 없을 때 401 {@code UNAUTHORIZED} 를 공통 에러 형식으로 내보낸다 (지시서 3장).
     *
     * <p>직접 JSON 을 만들지 않고 {@code GlobalExceptionHandler} 에 위임한다.
     * 그래야 {@code ErrorResponse} 형식이 다른 응답과 완전히 같아지고,
     * 날짜 형식(G1) 같은 설정도 그대로 따라온다.
     */
    private AuthenticationEntryPoint unauthorizedEntryPoint(HandlerExceptionResolver resolver) {
        return (request, response, authException) ->
                resolver.resolveException(request, response, null,
                        new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    /**
     * 인가 거부 처리. <b>401 {@code UNAUTHORIZED} 를 돌려준다.</b>
     *
     * <p>지시서 T08-2 는 "403 핸들러가 {@code UNAUTHORIZED} JSON" 이라고 했지만
     * {@code ErrorResponse.of} 가 ErrorCode 에서 상태를 뽑아내므로 403 과 UNAUTHORIZED(401)를
     * 동시에 만들 수 없다. 헤더와 본문이 어긋나면 프론트가 코드로 분기할 때 더 헷갈린다.
     *
     * <p>게다가 지금은 {@code anyRequest().authenticated()} 뿐이고 권한(role) 규칙이 없어서
     * {@code AccessDeniedException} 이 발생할 경로 자체가 없다. 이 앱의 실제 403
     * ({@code USER_WITHDRAWN}, {@code POST_NOT_OWNER})은 서비스에서 BusinessException 으로
     * 던져져 GlobalExceptionHandler 가 처리한다.
     *
     * <p>나중에 권한 체계가 생기면 그때 403 전용 코드를 지시서에 추가하고 여기를 고친다.
     */
    private AccessDeniedHandler accessDeniedHandler(HandlerExceptionResolver resolver) {
        return (request, response, accessDeniedException) ->
                resolver.resolveException(request, response, null,
                        new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** 여기서 등록해야 @Import(SecurityConfig.class) 를 쓰는 슬라이스 테스트에도 함께 들어간다. */
    @Bean
    public JwtTokenProvider jwtTokenProvider(JwtProperties jwtProperties) {
        return new JwtTokenProvider(jwtProperties);
    }
}
