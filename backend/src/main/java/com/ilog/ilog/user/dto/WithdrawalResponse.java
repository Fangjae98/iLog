package com.ilog.ilog.user.dto;

import com.ilog.ilog.user.domain.User;
import com.ilog.ilog.user.domain.UserPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 회원 탈퇴 응답 (MBR-08, U1).
 *
 * <p>프론트가 보관 기한(30일)을 코드에 적지 않고 이 값을 그대로 안내한다.
 */
public record WithdrawalResponse(
        @Schema(description = "이 시각 전까지 계정을 복구할 수 있다 (KST, 탈퇴 시각 + 30일)", example = "2026-10-28T10:00:00")
        LocalDateTime recoverableUntil
) {

    public static WithdrawalResponse from(User user) {
        return new WithdrawalResponse(user.getWithdrawnAt().plusDays(UserPolicy.WITHDRAWAL_RECOVERY_DAYS));
    }
}
