package com.commerce.api.global.outbox;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 기록/조회 서비스.
 *
 * <p>{@link #append}는 <b>호출자의 트랜잭션에 합류</b>한다(자체 @Transactional 없음) — 그래야 도메인 상태 변경과
 * 이벤트 INSERT가 한 커밋이 되어 원자성을 보장한다(아웃박스의 핵심).
 */
@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;

    /** 이벤트를 아웃박스에 기록한다(PENDING). 반드시 도메인 쓰기와 같은 트랜잭션 안에서 호출한다. */
    public void append(String eventType, String aggregateType, String aggregateId, String payload) {
        outboxRepository.save(OutboxEvent.pending(eventType, aggregateType, aggregateId, payload));
    }

    /**
     * 발행 후보 배치(생성순) — 폴러가 가져간다. PENDING이면서 백오프 대기가 끝난 이벤트만 포함한다.
     * 실제 행 잠금(중복 발행 방지)은 폴러가 건별 {@code publish}에서 SKIP LOCKED로 처리한다.
     */
    @Transactional(readOnly = true)
    public List<OutboxEvent> findPending() {
        return outboxRepository.findDispatchable(LocalDateTime.now());
    }
}
