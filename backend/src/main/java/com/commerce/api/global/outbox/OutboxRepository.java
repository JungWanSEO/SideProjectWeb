package com.commerce.api.global.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * 발행 후보 스캔(잠금 없음, 싼 조회) — PENDING이면서 백오프 대기가 끝난(next_attempt_at 도래) 이벤트를 생성순으로.
     *
     * <p>신규 이벤트는 {@code next_attempt_at IS NULL}이라 즉시 대상. 실패로 미뤄진 이벤트는 그 시각이 지나야 다시 잡힌다.
     * 실제 행 잠금(중복 발행 방지)은 {@link #findPendingForUpdate}가 건별로 담당한다.
     */
    @Query(value = """
        SELECT * FROM outbox_event
        WHERE status = 'PENDING'
          AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
        ORDER BY id ASC
        LIMIT 100
        """, nativeQuery = true)
    List<OutboxEvent> findDispatchable(@Param("now") LocalDateTime now);

    /**
     * 발행 직전 행 잠금 클레임 — {@code FOR UPDATE SKIP LOCKED}.
     *
     * <p>다른 폴러가 이미 이 행을 처리 중(=잠금 보유)이면 <b>빈 결과로 즉시 건너뛴다</b>(대기하지 않음) →
     * 폴러를 여러 개 띄워도 같은 이벤트를 두 번 발행하지 않는다. 잠금은 호출한 트랜잭션이 끝날 때까지 유지된다.
     *
     * <p>⚠️ {@code SKIP LOCKED}는 JPQL로 표현 불가 → native. H2(테스트)는 미지원이라 이 경로는 MySQL 런타임으로 검증한다.
     */
    @Query(value = """
        SELECT * FROM outbox_event
        WHERE id = :id AND status = 'PENDING'
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    Optional<OutboxEvent> findPendingForUpdate(@Param("id") Long id);
}
