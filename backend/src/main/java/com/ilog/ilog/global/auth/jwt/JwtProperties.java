package com.ilog.ilog.global.auth.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 설정. application.properties 의 ilog.jwt.* 와 묶인다.
 *
 * @param secret              HS256 서명 키. 커밋하지 않는다 (application-local.properties 또는 환경변수 ILOG_JWT_SECRET)
 * @param accessTokenValidity accessToken 유효 기간 (예: 2h)
 */
@ConfigurationProperties("ilog.jwt")
public record JwtProperties(String secret, Duration accessTokenValidity) {

    /** 로그에 서명 키가 남지 않도록 가린다. */
    @Override
    public String toString() {
        return "JwtProperties[secret=****, accessTokenValidity=" + accessTokenValidity + "]";
    }
}
