package com.ilog.ilog.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * 예약 작업(@Scheduled)과 그 작업이 쓰는 시계.
 *
 * <p>{@link Clock} 을 빈으로 두는 이유: "30일 경과" 같은 시각 판정을 테스트에서 고정된 시각으로 바꿔 끼우기 위해서다.
 *
 * <p>시간대는 JVM 기본값을 쓴다. 탈퇴 시각({@code withdrawn_at})·작성 시각 같은 저장 값이
 * {@code LocalDateTime.now()}(JVM 기본 시간대)로 채워지므로, 비교하는 쪽도 같은 시간대여야 어긋나지 않는다.
 * 여기만 KST 로 고정하면 기본 시간대가 UTC 인 컨테이너에서 판정이 9시간 틀어진다.
 * 서비스 시간을 KST 로 맞추는 것은 실행 환경(TZ)의 몫이다.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
