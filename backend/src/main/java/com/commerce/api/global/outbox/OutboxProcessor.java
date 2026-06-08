package com.commerce.api.global.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 아웃박스 이벤트를 한 건씩 트랜잭션으로 처리한다.
 *
 * <p>폴러({@link OutboxRelay})와 별도 빈으로 두는 이유: {@code @Transactional}은 프록시로 동작하므로
 * 같은 빈 내부 호출(self-invocation)이면 트랜잭션이 안 걸린다 → 폴러가 "다른 빈"인 이 프로세서를 호출해야
 * 이벤트별 트랜잭션 경계가 실제로 생긴다. (대표적인 Spring AOP 함정.)
 */
@Component
@RequiredArgsConstructor
public class OutboxProcessor {

    static final int MAX_RETRIES = 5;

    private final OutboxRepository outboxRepository;
    private final EventPublisher eventPublisher;

    /**
     * 발행 + PUBLISHED 표시를 <b>한 트랜잭션</b>으로. 핸들러 부수효과(예: 알림 저장)와 PUBLISHED 플래그가
     * 원자적으로 커밋된다 → "발행됐는데 핸들러 작업이 안 됨" 또는 그 반대가 생기지 않는다.
     * 핸들러가 던지면 예외가 전파되어 이 트랜잭션이 롤백되고(부수효과 취소), 폴러가 {@link #recordFailure}로 넘긴다.
     */
    @Transactional
    public void publish(Long id) {
        OutboxEvent event = outboxRepository.findById(id).orElse(null);
        if (event == null || event.getStatus() != OutboxStatus.PENDING) {
            return; // 이미 처리됨(동시성/중복 폴링 가드)
        }
        eventPublisher.publish(event);   // 핸들러 실행(같은 트랜잭션)
        event.markPublished();
    }

    /** 발행 실패 기록 — 별도 트랜잭션(폴러가 publish 롤백 후 호출). 재시도 누적, 초과 시 FAILED. */
    @Transactional
    public void recordFailure(Long id, String error) {
        outboxRepository.findById(id).ifPresent(event -> event.recordFailure(error, MAX_RETRIES));
    }
}
