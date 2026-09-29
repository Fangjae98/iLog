package com.ilog.ilog.user.domain;

import com.ilog.ilog.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 회원.
 *
 * created_at / updated_at 은 BaseTimeEntity 가 JPA Auditing 으로 채운다.
 * (global 의 JpaAuditingConfig 가 modifyOnCreate=false 라 생성 시 updated_at 은 null)
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 가 요구하는 기본 생성자. 외부 호출은 막는다.
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** USER_DUPLICATE_EMAIL / LOGIN_FAILED 판정 대상. RFC 5321 상한이 254자. */
    @Column(nullable = false, length = 254)
    private String email;

    /** USER_DUPLICATE_NICKNAME / NICKNAME_UNCHANGED 판정 대상. */
    @Column(nullable = false, length = 20)
    private String nickname;

    /** BCrypt 해시(60자 고정). 평문을 절대 저장하지 않는다. */
    @Column(nullable = false, length = 60)
    private String password;

    /** 실명. 수정 불가, 마이페이지(비밀번호 재확인 후)와 임시 비밀번호 발급 본인 확인에만 쓴다. */
    @Column(nullable = false, length = 50)
    private String name;

    /** 임시 비밀번호로 로그인한 상태인지. global 의 LoginUser.tempPassword 와 대응. */
    @Column(name = "temp_password", nullable = false)
    private boolean tempPassword;

    /** 임시 비밀번호 만료 시각 (U6, 발급 후 24시간). 임시 비밀번호 상태가 아니면 null. */
    @Column(name = "temp_password_expires_at")
    private LocalDateTime tempPasswordExpiresAt;

    /** 탈퇴 시각. null 이면 활성 회원. USER_WITHDRAWN / REJOIN_RESTRICTED 판정에 사용. */
    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @Builder
    private User(String email, String nickname, String password, String name) {
        this.email = email;
        this.nickname = nickname;
        this.password = password;
        this.name = name;
        this.tempPassword = false;
    }

    public boolean isWithdrawn() {
        return withdrawnAt != null;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    /** 인코딩이 끝난 해시를 받는다. 인코딩은 서비스 계층에서 PasswordEncoder 로. */
    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.tempPassword = false;
        this.tempPasswordExpiresAt = null;
    }

    /**
     * 임시 비밀번호로 교체한다 (AUTH-03).
     * 만료 시각을 함께 받는다 (U6: 발급 후 24시간). 만료 검사는 로그인에서 한다.
     */
    public void issueTempPassword(String encodedTempPassword, LocalDateTime expiresAt) {
        this.password = encodedTempPassword;
        this.tempPassword = true;
        this.tempPasswordExpiresAt = expiresAt;
    }

    /** 임시 비밀번호 상태이면서 만료 시각이 지났는지 (U6). */
    public boolean isTempPasswordExpired(LocalDateTime now) {
        return tempPassword && tempPasswordExpiresAt != null && tempPasswordExpiresAt.isBefore(now);
    }

    public void withdraw(LocalDateTime withdrawnAt) {
        this.withdrawnAt = withdrawnAt;
    }

    /** 탈퇴를 취소한다 (AUTH-04). 30일 경과 여부는 서비스에서 먼저 확인한다. */
    public void restore() {
        this.withdrawnAt = null;
    }
}
