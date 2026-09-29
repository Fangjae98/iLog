package com.ilog.ilog.user.repository;

import com.ilog.ilog.user.domain.PasswordHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistory, Long> {

    /**
     * 최신 이력부터 {@code pageable} 개수만큼.
     *
     * <p>{@code created_at} 이 아니라 {@code id} 로 정렬한다. IDENTITY 는 단조 증가라 순서가 확실한데,
     * {@code created_at} 은 같은 밀리초에 여러 건이 들어가면 순서가 흔들린다.
     */
    List<PasswordHistory> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /** 남길 id 를 빼고 지운다. JPQL 에 LIMIT 이 없어서 "최신 N 건을 고른 뒤 나머지 삭제" 형태를 쓴다. */
    void deleteByUserIdAndIdNotIn(Long userId, Collection<Long> keepIds);
}
