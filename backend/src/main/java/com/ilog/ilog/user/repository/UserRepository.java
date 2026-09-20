package com.ilog.ilog.user.repository;

import com.ilog.ilog.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /** 로그인. LOGIN_FAILED 판정에 사용. */
    Optional<User> findByEmail(String email);

    /** 가입 시 MEMBER_DUPLICATE_EMAIL 판정. */
    boolean existsByEmail(String email);

    /** 가입·닉네임 변경 시 MEMBER_DUPLICATE_NICKNAME 판정. */
    boolean existsByNickname(String nickname);
}
