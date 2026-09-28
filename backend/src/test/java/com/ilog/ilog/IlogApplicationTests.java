package com.ilog.ilog;

import com.ilog.ilog.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

/**
 * 스프링 컨텍스트가 뜨는지 확인한다.
 *
 * <p>전에는 {@code @SpringBootTest} 만 붙어 있어서 로컬에 PostgreSQL 이 떠 있어야만 통과했다.
 * 이제 {@link DatabaseTestSupport} 의 컨테이너를 쓰고, Docker 가 없으면 건너뛴다 (T02-5).
 */
class IlogApplicationTests extends DatabaseTestSupport {

	@Test
	void contextLoads() {
	}

}
