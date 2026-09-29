package com.ilog.ilog.auth.service;

import com.ilog.ilog.user.domain.UserPolicy;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 임시 비밀번호 생성기 (AUTH-03).
 *
 * <p>무작위라 한 번 통과하는 것으로는 부족하다. 생성한 값이 <b>항상</b>
 * 비밀번호 규칙을 만족해야 받은 사람이 바로 로그인할 수 있다.
 */
class TemporaryPasswordGeneratorTest {

    private final TemporaryPasswordGenerator generator = new TemporaryPasswordGenerator();

    @RepeatedTest(50)
    void 생성한_비밀번호는_언제나_규칙을_만족한다() {
        String generated = generator.generate();

        assertThat(generated).hasSize(UserPolicy.TEMP_PASSWORD_LENGTH);
        assertThat(UserPolicy.isValidPassword(generated))
                .as("생성값 '%s' 가 비밀번호 규칙을 만족하지 않습니다", generated)
                .isTrue();
    }

    @Test
    void 부를_때마다_다른_값이_나온다() {
        Set<String> generated = new HashSet<>();
        IntStream.range(0, 100).forEach(i -> generated.add(generator.generate()));

        // 12자 무작위라 100번 안에 겹칠 확률은 사실상 0이다
        assertThat(generated).hasSize(100);
    }

    @Test
    void 헷갈리는_문자는_쓰지_않는다() {
        // 메일로 받아 손으로 옮겨 적는 값이라 0/O, 1/l/I 를 뺐다
        String allGenerated = IntStream.range(0, 200)
                .mapToObj(i -> generator.generate())
                .reduce("", String::concat);

        assertThat(allGenerated).doesNotContain("0", "O", "1", "l", "I", "o");
    }
}
