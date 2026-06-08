package com.commerce.api.global.outbox;

import java.time.Duration;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

    /** 백오프 기본 간격(ms) — 1회 실패 후 대기. 이후 2배씩 증가. */
    @Value("${outbox.relay.backoff-base-ms:2000}")
    private long backoffBaseMs;

    /** 백오프 상한(ms) — 지수 증가가 무한정 커지지 않게 cap. */
    @Value("${outbox.relay.backoff-max-ms:60000}")
    private long backoffMaxMs;

    /**
     * 발행 + PUBLISHED 표시를 <b>한 트랜잭션</b>으로. 핸들러 부수효과(예: 알림 저장)와 PUBLISHED 플래그가
     * 원자적으로 커밋된다 → "발행됐는데 핸들러 작업이 안 됨" 또는 그 반대가 생기지 않는다.
     * 핸들러가 던지면 예외가 전파되어 이 트랜잭션이 롤백되고(부수효과 취소), 폴러가 {@link #recordFailure}로 넘긴다.
     *
     * <p>행 조회는 {@code FOR UPDATE SKIP LOCKED}({@link OutboxRepository#findPendingForUpdate}) — 다른 폴러가
     * 이미 처리 중이면 빈 결과로 즉시 스킵한다(스케일아웃 시 중복 발행 방지). 이 잠금은 본 트랜잭션 동안 유지된다.
     */
    @Transactional
    public void publish(Long id) {
        OutboxEvent event = outboxRepository.findPendingForUpdate(id).orElse(null);
        if (event == null) {
            return; // 이미 처리됨 또는 다른 폴러가 처리 중(잠금) → 스킵
        }
        eventPublisher.publish(event);   // 핸들러 실행(같은 트랜잭션)
        event.markPublished();
    }

    /** 발행 실패 기록 — 별도 트랜잭션(폴러가 publish 롤백 후 호출). 재시도 누적·지수 백오프, 초과 시 FAILED. */
    @Transactional
    public void recordFailure(Long id, String error) {
        outboxRepository.findById(id)
            .ifPresent(event -> event.recordFailure(error, MAX_RETRIES, nextAttemptAfter(event.getRetryCount())));
    }

    /**
     * 지수 백오프 시각 계산 — {@code now + min(base · 2^retryCount, max)}.
     *
     * <p>{@code retryCount}는 증가 <i>전</i> 값(0,1,2,3…) → 대기 2s, 4s, 8s, 16s … 상한까지. (.NET Polly의
     * 지수 백오프와 같은 사고.)
     */
    private LocalDateTime nextAttemptAfter(int retryCount) {
        long delayMs = Math.min(backoffBaseMs << retryCount, backoffMaxMs);
        return LocalDateTime.now().plus(Duration.ofMillis(delayMs));
    }
}
