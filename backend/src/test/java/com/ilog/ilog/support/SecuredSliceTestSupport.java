package com.ilog.ilog.support;

import com.ilog.ilog.global.auth.ActiveUserChecker;
import com.ilog.ilog.global.auth.jwt.JwtTokenProvider;
import com.ilog.ilog.global.config.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

/**
 * 인증이 걸린 컨트롤러 슬라이스 테스트의 공통 기반 (T08).
 *
 * <p>{@code permitAll} 을 푼 뒤로는 슬라이스 테스트마다 두 가지가 필요하다.
 * <ul>
 *   <li>진짜 Bearer 토큰 — 개발용 {@code X-User-Id} 헤더가 없어졌다</li>
 *   <li>{@link ActiveUserChecker} 목 — {@code SecurityConfig} 가 이 빈을 요구하는데
 *       {@code @WebMvcTest} 는 {@code @Component} 를 스캔하지 않는다</li>
 * </ul>
 *
 * <p>목의 기본 반환값이 {@code false} 라 스텁을 빠뜨리면 <b>모든 인증 요청이 401</b> 이 되고
 * 원인을 찾기 어렵다. 그래서 여기서 한 번에 켜 둔다. 탈퇴 회원을 흉내 내는 테스트는
 * {@code given(activeUserChecker.isActive(1L)).willReturn(false)} 로 덮어쓰면 된다.
 */
@Import(SecurityConfig.class)
public abstract class SecuredSliceTestSupport {

    @Autowired
    protected JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    protected ActiveUserChecker activeUserChecker;

    @BeforeEach
    void 기본적으로_모든_회원을_활성으로_본다() {
        given(activeUserChecker.isActive(anyLong())).willReturn(true);
    }

    /** {@code Authorization} 헤더에 그대로 넣을 값. */
    protected String bearer(long userId) {
        return bearer(userId, false);
    }

    /** 임시 비밀번호 상태(tmp=true) 토큰이 필요할 때 쓴다 (A8 확인용). */
    protected String bearer(long userId, boolean tempPassword) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId, tempPassword);
    }
}
