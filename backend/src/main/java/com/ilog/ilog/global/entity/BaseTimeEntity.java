package com.ilog.ilog.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")          // 생성 시 null, 수정 시 기록
    private LocalDateTime updatedAt;

    /**
     * 값이 바뀌지 않은 수정 요청에서도 수정 일시를 남긴다 (게시글 P6).
     * {@code @LastModifiedDate} 는 엔티티가 실제로 바뀌어 UPDATE 가 나갈 때만 채워지기 때문이다.
     */
    protected void markUpdated() {
        this.updatedAt = LocalDateTime.now();
    }
}
