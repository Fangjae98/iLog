package com.ilog.ilog.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 예약 작업(@Scheduled)과 그 작업이 쓰는 시계.
 *
 * <p>{@link Clock} 을 빈으로 두는 이유: "30일 경과" 같은 시각 판정을 테스트에서 고정된 시각으로 바꿔 끼우기 위해서다.
 * 서버 기본 시간대와 상관없이 KST 로 계산한다 (DB 의 timestamp 도 KST, application.properties 참고).
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
