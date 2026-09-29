package com.ilog.ilog.global.auth.jwt;

import com.ilog.ilog.global.auth.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * accessToken 발급과 검증.
 *
 * 빈 등록은 SecurityConfig 의 @Bean 으로 한다. @Import(SecurityConfig.class) 를 쓰는
 * 컨트롤러 슬라이스 테스트가 따로 설정하지 않아도 이 빈을 받도록 하기 위해서다.
 */
public class JwtTokenProvider {

    /** HS256 은 256비트(32바이트) 이상의 키를 요구한다. */
    private static final int MIN_SECRET_BYTES = 32;
    private static final String TEMP_PASSWORD_CLAIM = "tmp";

    private final SecretKey key;
    private final JwtParser parser;
    private final Duration accessTokenValidity;
    private final Clock clock;

    public JwtTokenProvider(JwtProperties properties) {
        this(properties, Clock.systemUTC());
    }

    /** 테스트에서 시각을 고정하려고 Clock 을 받는다. */
    JwtTokenProvider(JwtProperties properties, Clock clock) {
        this.key = Keys.hmacShaKeyFor(validSecret(properties.secret()));
        this.accessTokenValidity = validValidity(properties.accessTokenValidity());
        this.clock = clock;
        this.parser = Jwts.parser()
                .verifyWith(key)
                .clock(() -> Date.from(clock.instant()))
                .build();
    }

    /** sub = 회원 ID, tmp = 임시 비밀번호 로그인 여부. */
    public String createAccessToken(Long userId, boolean tempPassword) {
        Instant now = clock.instant();
        return Jwts.builder()
                .subject(userId.toString())
                .claim(TEMP_PASSWORD_CLAIM, tempPassword)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenValidity)))
                .signWith(key, Jwts.SIG.HS256)   // 생략하면 키 길이에 따라 HS384·HS512 로 바뀐다
                .compact();
    }

    /** 로그인 응답의 expiresIn (초). */
    public long accessTokenValiditySeconds() {
        return accessTokenValidity.toSeconds();
    }

    /**
     * 서명·만료·클레임을 확인해 LoginUser 로 복원한다.
     * 위조·만료·형식 오류는 이유를 구분하지 않고 empty 로 돌려준다 (모두 401 UNAUTHORIZED 로 처리하기로 함).
     */
    public Optional<LoginUser> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = parser.parseSignedClaims(token).getPayload();
            Boolean tempPassword = claims.get(TEMP_PASSWORD_CLAIM, Boolean.class);
            if (claims.getSubject() == null || tempPassword == null) {
                return Optional.empty();
            }
            return Optional.of(new LoginUser(Long.valueOf(claims.getSubject()), tempPassword));
        } catch (JwtException | IllegalArgumentException e) {   // NumberFormatException 도 여기에 포함된다
            return Optional.empty();
        }
    }

    private static byte[] validSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("ilog.jwt.secret 이 비어 있습니다. "
                    + "로컬은 application-local.properties 에, 도커는 .env 의 JWT_SECRET 에 32바이트 이상의 값을 넣으세요. "
                    + "(생성: openssl rand -base64 48)");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("ilog.jwt.secret 은 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다. "
                    + "(현재 " + bytes.length + "바이트, 생성: openssl rand -base64 48)");
        }
        return bytes;
    }

    private static Duration validValidity(Duration validity) {
        if (validity == null || validity.isNegative() || validity.isZero()) {
            throw new IllegalStateException("ilog.jwt.access-token-validity 는 0보다 커야 합니다. (예: 2h)");
        }
        return validity;
    }
}
