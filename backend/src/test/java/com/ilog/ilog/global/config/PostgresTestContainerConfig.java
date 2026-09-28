package com.ilog.ilog.global.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트가 쓸 PostgreSQL 컨테이너 설정.
 *
 * 이 설정을 @Import 한 테스트는 각자 PC 에 깔린 DB 가 아니라 여기서 띄운 컨테이너에 붙는다.
 * 그래서 누가 돌려도, application-local.properties 가 있든 없든 결과가 같다.
 *
 * 이미지 태그는 docker-compose.yml 과 똑같이 16 으로 못박았다.
 * Testcontainers 의 기본 태그는 우리가 쓰는 버전이 아니므로 생성자에 직접 넘긴다.
 *
 * @ServiceConnection 이 컨테이너의 접속 정보로 JdbcConnectionDetails 빈을 만들어 준다.
 * 스프링은 이 빈이 있으면 spring.datasource.* 설정을 읽지 않는다
 * (DataSourceAutoConfiguration 의 기본 구현이 @ConditionalOnMissingBean(JdbcConnectionDetails) 이다).
 * 포트도 컨테이너마다 무작위로 잡히므로 5432 를 쓰는 docker compose 의 DB 와 부딪히지 않는다.
 *
 * @TestConfiguration 은 컴포넌트 스캔에서 제외된다.
 * @Import 로 불러온 테스트에서만 동작하고 @WebMvcTest 같은 슬라이스 테스트에는 끼어들지 않는다.
 *
 * 주의: Spring Boot 4.1.1 이 관리하는 Testcontainers 는 2.0.5 다.
 * 인터넷 예제 대부분은 1.x 라서 좌표(org.testcontainers:postgresql)와
 * 클래스(org.testcontainers.containers.PostgreSQLContainer)가 다르다. 2.x 에서는 둘 다 쓰지 않는다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class PostgresTestContainerConfig {

    /** 팀 표준 DB. docker-compose.yml 의 postgres:16 과 같은 이미지다. 버전을 올릴 땐 여기와 compose 를 같이 고친다. */
    private static final DockerImageName POSTGRES_16 = DockerImageName.parse("postgres:16");

    /*
     * 컨테이너를 static 필드로 잡아 두지 않는다.
     * 스프링 테스트 컨텍스트는 JVM 안에서 캐시되므로, 이 설정을 쓰는 테스트 클래스가 여러 개가 되어도
     * 설정이 같으면 컨텍스트가 재사용되고 컨테이너도 한 번만 뜬다.
     * static 으로 잡아 두면 컨텍스트 하나가 닫힐 때 다른 테스트가 아직 쓰는 컨테이너를 멈출 수 있다.
     * 테스트가 끝나면 Testcontainers 의 ryuk 컨테이너가 알아서 정리한다.
     */
    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return new PostgreSQLContainer(POSTGRES_16);
    }
}
