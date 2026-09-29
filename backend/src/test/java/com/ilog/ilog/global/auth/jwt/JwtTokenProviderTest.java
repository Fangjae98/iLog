package com.ilog.ilog.global.auth.jwt;

import com.ilog.ilog.global.auth.LoginUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "test-only-jwt-secret-0123456789-abcdefghij";
    private static final Duration VALIDITY = Duration.ofHours(2);

    JwtTokenProvider provider = new JwtTokenProvider(new JwtProperties(SECRET, VALIDITY));

    @Test
    void 발급한_토큰을_다시_LoginUser로_복원한다() {
        assertThat(provider.parse(provider.createAccessToken(7L, false))).contains(new LoginUser(7L, false));
        assertThat(provider.parse(provider.createAccessToken(8L, true))).contains(new LoginUser(8L, true));
    }

    @Test
    void 키가_길어도_HS256으로_서명한다() {
        JwtTokenProvider longKey = new JwtTokenProvider(new JwtProperties("x".repeat(64), VALIDITY));
        String header = new String(Base64.getUrlDecoder().decode(longKey.createAccessToken(1L, false).split("\\.")[0]),
                StandardCharsets.UTF_8);

        assertThat(header).contains("\"alg\":\"HS256\"");
    }

    @Test
    void 유효_시간은_초_단위로_돌려준다() {
        assertThat(provider.accessTokenValiditySeconds()).isEqualTo(7200);
    }

    @Test
    void 만료된_토큰은_거부한다() {
        Instant issuedAt = Instant.parse("2026-09-21T00:00:00Z");
        JwtTokenProvider past = new JwtTokenProvider(new JwtProperties(SECRET, VALIDITY), Clock.fixed(issuedAt, ZoneOffset.UTC));
        String token = past.createAccessToken(1L, false);

        JwtTokenProvider justBefore = new JwtTokenProvider(new JwtProperties(SECRET, VALIDITY),
                Clock.fixed(issuedAt.plus(VALIDITY).minusSeconds(1), ZoneOffset.UTC));
        JwtTokenProvider after = new JwtTokenProvider(new JwtProperties(SECRET, VALIDITY),
                Clock.fixed(issuedAt.plus(VALIDITY).plusSeconds(1), ZoneOffset.UTC));

        assertThat(justBefore.parse(token)).isPresent();
        assertThat(after.parse(token)).isEmpty();
    }

    @Test
    void 다른_키로_서명한_토큰은_거부한다() {
        JwtTokenProvider other = new JwtTokenProvider(new JwtProperties("another-secret-0123456789-0123456789-xyz", VALIDITY));

        assertThat(provider.parse(other.createAccessToken(1L, false))).isEmpty();
    }

    @Test
    void 변조한_토큰은_거부한다() {
        String token = provider.createAccessToken(1L, false);
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"2\",\"tmp\":false}".getBytes(StandardCharsets.UTF_8));

        assertThat(provider.parse(parts[0] + "." + forgedPayload + "." + parts[2])).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "garbage", "a.b.c"})
    void 형식이_틀린_토큰은_거부한다(String token) {
        assertThat(provider.parse(token)).isEmpty();
    }

    @Test
    void 서명은_맞아도_클레임이_이상하면_거부한다() {
        var key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date exp = Date.from(Instant.now().plus(VALIDITY));

        String notNumberSub = Jwts.builder().subject("abc").claim("tmp", false).expiration(exp).signWith(key).compact();
        String noTmp = Jwts.builder().subject("1").expiration(exp).signWith(key).compact();
        String noSub = Jwts.builder().claim("tmp", false).expiration(exp).signWith(key).compact();

        assertThat(provider.parse(notNumberSub)).isEmpty();
        assertThat(provider.parse(noTmp)).isEmpty();
        assertThat(provider.parse(noSub)).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "0123456789012345678901234567890"})   // 마지막은 31바이트
    void 서명_키가_없거나_32바이트보다_짧으면_서버가_뜨지_않는다(String secret) {
        assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties(secret, VALIDITY)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ilog.jwt.secret");
    }

    @Test
    void 유효_시간이_없거나_0이면_서버가_뜨지_않는다() {
        assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties(SECRET, null)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties(SECRET, Duration.ZERO)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 설정을_로그로_찍어도_서명_키는_나오지_않는다() {
        assertThat(new JwtProperties(SECRET, VALIDITY).toString()).doesNotContain(SECRET);
    }
}
