package com.ilog.ilog.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 비밀번호 재사용 금지를 위한 이력 (U7, D-13).
 *
 * <p>회원당 최신 {@link UserPolicy#PASSWORD_HISTORY_LIMIT} 건만 남긴다.
 * 임시 비밀번호는 이력에 넣지 않는다 (U7).
 *
 * <p>{@code BaseTimeEntity} 를 상속하지 않는다. 지시서 6장 스키마에 이 테이블은
 * {@code created_at} 만 있고, 이력 행은 수정되지 않아 {@code updated_at} 이 의미가 없다.
 *
 * <p>{@code @OnDelete} 는 DDL 생성에만 영향을 준다. {@code ddl-auto=update} 는 이미 있는
 * 테이블의 제약을 바꾸지 못할 수 있어서, 회원 물리 삭제(T12)는 DB 의 CASCADE 에 기대지 않고
 * 벌크 쿼리로 직접 지운다.
 */
@Entity
@Getter
@Table(name = "password_history")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    /** BCrypt 해시(60자 고정). 평문을 절대 저장하지 않는다. */
    @Column(name = "password_hash", nullable = false, length = 60)
    private String passwordHash;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public PasswordHistory(User user, String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }
}
