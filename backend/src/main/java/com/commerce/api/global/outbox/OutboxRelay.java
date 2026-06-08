package com.commerce.api.global.outbox;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 아웃박스 폴러(relay) — 주기적으로 미발행 이벤트를 발행한다.
 *
 * <p>흐름: PENDING 배치 조회(생성순) → 각 이벤트를 {@link OutboxProcessor#publish}(이벤트별 트랜잭션)로 발행.
 * 실패하면 {@code recordFailure}로 재시도 횟수를 올린다(다음 tick 재시도, 초과 시 FAILED).
 *
 * <p>이 빈 자체는 트랜잭션이 없다 — 트랜잭션 경계는 프로세서(별도 빈) 호출마다 생긴다(self-invocation 회피).
 *
 * <p>테스트에선 {@code outbox.relay.enabled=false}로 끈다(스케줄러가 테스트 중 도는 것을 방지).
 */
@Component
@ConditionalOnProperty(name = "outbox.relay.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxService outboxService;
    private final OutboxProcessor outboxProcessor;

    @Scheduled(fixedDelayString = "${outbox.relay.delay-ms:2000}")
    public void relay() {
        for (OutboxEvent event : outboxService.findPending()) {
            try {
                outboxProcessor.publish(event.getId());
            } catch (RuntimeException ex) {
                // 발행 트랜잭션은 롤백됨 → 실패를 별도 트랜잭션으로 기록(재시도 누적)
                outboxProcessor.recordFailure(event.getId(), ex.getMessage());
            }
        }
    }
}
