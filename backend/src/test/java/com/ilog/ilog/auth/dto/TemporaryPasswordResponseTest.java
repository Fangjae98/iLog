package com.ilog.ilog.auth.dto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/** 이메일 가리기 규칙 (U8): @ 앞 첫 글자 + *** + @도메인 */
class TemporaryPasswordResponseTest {

    @ParameterizedTest
    @CsvSource({
            "hong.gildong@example.co.kr, h***@example.co.kr",
            "user@example.com,     u***@example.com",
            "a@b.com,              a***@b.com",          // 로컬 파트가 1글자여도 길이를 드러내지 않는다
            "kim.tae-k@example.org,  k***@example.org"
    })
    void 이메일은_첫_글자만_남기고_가린다(String email, String expected) {
        assertThat(TemporaryPasswordResponse.of(email).email()).isEqualTo(expected);
    }

    @ParameterizedTest
    @CsvSource({"'@b.com'", "'no-at-sign'"})
    void 형식이_이상하면_통째로_가린다(String email) {
        assertThat(TemporaryPasswordResponse.of(email).email()).isEqualTo("***");
    }
}
