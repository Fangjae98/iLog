package com.ilog.ilog.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 실제 PostgreSQL 이 필요한 테스트의 공통 기반 (T02-5).
 *
 * <p>ILIKE, FK CASCADE, 스케줄러 벌크 삭제처럼 H2 로는 확인할 수 없는 동작을
 * 명세와 같은 {@code postgres:16} 에서 검증하기 위한 것이다.
 *
 * <p><b>컨테이너는 JVM 당 하나만 띄우고 내리지 않는다(싱글턴).</b>
 * {@code @Container} 로 두면 테스트 클래스마다 시작·종료하는데, Spring 은 같은 설정의
 * 컨텍스트를 캐시해서 재사용한다. 그래서 첫 클래스가 끝나며 컨테이너를 내리면
 * 캐시된 컨텍스트가 죽은 포트를 계속 가리켜 다음 클래스가 연결 거부로 실패한다.
 * 정리는 Testcontainers 의 Ryuk 컨테이너가 JVM 종료 후 맡는다.
 *
 * <p>Docker 가 없으면 실패가 아니라 건너뛴다({@code disabledWithoutDocker}).
 * static 초기화에서 {@code isDockerAvailable} 을 먼저 보는 이유도 같다 —
 * 확인 없이 {@code start()} 를 부르면 Docker 없는 환경에서 skip 대신 오류가 난다.
 *
 * <p>{@code @ServiceConnection} 이 등록하는 {@code JdbcConnectionDetails} 빈이
 * {@code application-local.properties} 의 datasource 설정보다 우선하므로,
 * 로컬에 DB 가 떠 있든 아니든 항상 이 컨테이너를 쓴다.
 *
 * <p>{@code @Testcontainers} 와 {@code @SpringBootTest} 모두 {@code @Inherited} 라
 * 상속받는 쪽은 자기 테스트만 쓰면 된다.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class DatabaseTestSupport {

    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
        }
    }
}
