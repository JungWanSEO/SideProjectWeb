package com.commerce.api.global.outbox;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {

    /** 미발행 이벤트를 생성순으로 배치 조회 — 폴러가 순서대로 발행한다. */
    List<OutboxEvent> findTop100ByStatusOrderByIdAsc(OutboxStatus status);
}
