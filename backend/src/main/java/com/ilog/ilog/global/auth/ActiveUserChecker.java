package com.ilog.ilog.global.auth;

/**
 * 토큰의 회원이 아직 쓸 수 있는 계정인지 확인한다 (A6).
 *
 * <p>탈퇴하면 이미 발급된 토큰은 만료까지 살아 있다. 토큰 저장소가 없어서 폐기할 수 없으니
 * 요청마다 회원 상태를 보고 막는다. PK 조회 1회라 비용은 무시할 수준이다.
 *
 * <p>구현은 {@code user} 패키지에 둔다. {@code global} 이 {@code user} 를 import 하지 않도록
 * 인터페이스만 여기에 둔다.
 */
public interface ActiveUserChecker {

    /** 회원이 존재하고 탈퇴하지 않았으면 true. */
    boolean isActive(Long userId);
}
