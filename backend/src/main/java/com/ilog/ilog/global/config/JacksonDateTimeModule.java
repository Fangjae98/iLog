package com.ilog.ilog.global.config;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 응답의 날짜·시간 형식을 {@code yyyy-MM-dd'T'HH:mm:ss} 로 고정한다 (G1).
 *
 * <p>기본값은 {@code DateTimeFormatter.ISO_LOCAL_DATE_TIME} 이라 소수점 초가 붙는다
 * ({@code 2026-09-20T15:18:38.997289}). 명세 1장은 소수점 없는 형식을 쓴다.
 *
 * <p><b>{@code spring.jackson.date-format} 으로는 안 된다.</b> 그 설정은
 * {@code defaultDateFormat(SimpleDateFormat)} 으로만 흘러가서 {@link java.util.Date} 에만 먹고,
 * Jackson 3 의 {@code LocalDateTimeSerializer} 는 자기 {@code _formatter} 만 본다.
 *
 * <p><b>{@code @Configuration} 의 {@code @Bean} 이 아니라 {@code @Component} 인 이유</b>:
 * {@code @WebMvcTest} 슬라이스는 커스터마이저 빈을 집어가지 않지만,
 * {@code JacksonModule} 타입인 {@code @Component} 는 슬라이스에도 포함되어
 * Jackson 자동 설정이 그대로 적용해 준다. 컨트롤러 테스트에서도 같은 형식이 나와야 하므로 이 방식을 쓴다.
 *
 * <p>직렬화만 등록한다. 요청 DTO 에 {@code LocalDateTime} 이 없고, 패턴을 고정한 역직렬화기를 넣으면
 * 나중에 정상적인 ISO 입력을 거부하게 된다. {@code LocalDate} 는 기본값이 이미 {@code yyyy-MM-dd} 라 손대지 않는다.
 *
 * <p>KST 변환은 {@code spring.jackson.time-zone} 과 {@code hibernate.jdbc.time_zone} 담당이고,
 * 이 모듈은 글자 모양만 정한다.
 */
@Component
public class JacksonDateTimeModule extends SimpleModule {

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public JacksonDateTimeModule() {
        super("ilogDateTimeModule");
        addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME_FORMAT));
    }
}
