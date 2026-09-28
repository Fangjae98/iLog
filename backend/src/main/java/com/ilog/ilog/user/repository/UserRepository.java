package com.ilog.ilog.user.repository;

import com.ilog.ilog.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 로그인. LOGIN_FAILED 판정에 사용. */
    Optional<User> findByEmail(String email);

    /** 가입 시 USER_DUPLICATE_EMAIL 판정. */
    boolean existsByEmail(String email);

    /** 가입·닉네임 변경 시 USER_DUPLICATE_NICKNAME 판정. */
    boolean existsByNickname(String nickname);

    /**
     * 임시 비밀번호 발급 대상 조회 (AUTH-03, U8).
     * 탈퇴 회원은 제외한다 — 못 찾은 것과 같은 404 로 응답해 계정 상태를 드러내지 않는다.
     */
    Optional<User> findByEmailAndNameAndWithdrawnAtIsNull(String email, String name);
}
