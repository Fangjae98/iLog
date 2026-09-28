package com.ilog.ilog;

import com.ilog.ilog.global.config.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

/**
 * 애플리케이션 컨텍스트가 끝까지 뜨는지 확인한다.
 *
 * 나머지 테스트는 전부 슬라이스(@WebMvcTest)라 DB 를 쓰지 않는다.
 * 여기서만 진짜 DB 에 붙어서 다음을 한 번에 검사한다.
 *   - 엔티티 매핑이 PostgreSQL 16 에서 실제로 DDL 로 만들어지는지 (ddl-auto=update)
 *   - 리포지터리 메서드 이름으로 쿼리가 만들어지는지
 *   - Security · WebMvc · springdoc · JPA Auditing 설정이 서로 충돌하지 않는지
 *
 * DB 는 PostgresTestContainerConfig 가 띄우는 postgres:16 컨테이너다.
 *
 * @ActiveProfiles("test")
 *   application.properties 의 spring.profiles.active=local 을 덮어쓴다.
 *   local 프로필은 각자 PC 의 application-local.properties 를 끌어오는데,
 *   그 파일은 커밋되지 않아 사람마다 내용이 다르다. 테스트는 그 영향을 받지 않아야 한다.
 *   application-test.properties 는 따로 두지 않는다. 없어도 된다.
 *
 * @EnabledIfDockerAvailable
 *   Docker 가 꺼져 있으면 실패가 아니라 건너뛴다.
 *   테스트가 항상 빨간 상태로 있으면 진짜 회귀가 묻히기 때문이다.
 *   리포트에 skipped 로 나오면 Docker 를 켜고 다시 돌려야 실제로 검사된다.
 */
@SpringBootTest
@Import(PostgresTestContainerConfig.class)
@ActiveProfiles("test")
@EnabledIfDockerAvailable
class IlogApplicationTests {

    @Test
    void contextLoads() {
    }

}
