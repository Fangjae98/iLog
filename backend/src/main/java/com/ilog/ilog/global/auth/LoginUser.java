package com.ilog.ilog.global.auth;

/** 컨트롤러에 주입되는 로그인 회원 정보. JWT 클레임에서 복원한다. */
public record LoginUser(Long userId, boolean tempPassword) {
}
