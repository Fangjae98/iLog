package com.ilog.ilog.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 실제 PostgreSQL 이 필요한 테스트의 공통 기반 (T02-5).
 *
 * <p>ILIKE, FK CASCADE, 스케줄러 벌크 삭제처럼 H2 로는 확인할 수 없는 동작을
 * 명세와 같은 {@code postgres:16} 에서 검증하기 위한 것이다.
 *
 * <p>{@code @ServiceConnection} 이 등록하는 {@code JdbcConnectionDetails} 빈이
 * {@code application-local.properties} 의 datasource 설정보다 우선하므로,
 * 로컬에 DB 가 떠 있든 아니든 항상 이 컨테이너를 쓴다.
 *
 * <p><b>Docker 가 없으면 실패가 아니라 건너뛴다</b>({@code disabledWithoutDocker}).
 * 지시서 T02-5 의 요구사항이고, CI·팀원 환경마다 Docker 유무가 달라서 필요하다.
 *
 * <p>{@code @Testcontainers} 와 {@code @SpringBootTest} 모두 {@code @Inherited} 라
 * 상속받는 쪽은 자기 테스트만 쓰면 된다.
 *
 * <p>컨테이너는 테스트 클래스마다 뜨고 내려간다. DB 테스트 클래스가 많아져 느려지면
 * 그때 싱글턴 컨테이너 방식으로 바꾼다(지금 미리 할 이유는 없다).
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
public abstract class DatabaseTestSupport {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");
}
