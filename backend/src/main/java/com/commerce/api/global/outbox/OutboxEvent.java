package com.commerce.api.global.outbox;

import com.commerce.api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아웃박스 이벤트 — "상태 변경과 같은 트랜잭션에 기록되는 발행 대기 이벤트".
 *
 * <p>트랜잭셔널 아웃박스의 핵심: 도메인 상태(예: 결제 PAID)와 이 행이 <b>한 DB 트랜잭션</b>으로 커밋되어
 * 원자성을 보장하고, 폴러가 나중에 발행한다(at-least-once). 설계: docs/event-outbox-design.md.
 */
@Getter
@Entity
@Table(name = "outbox_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType;        // 예: "PAYMENT_COMPLETED"

    @Column(length = 50)
    private String aggregateType;    // 예: "PAYMENT" (라우팅/디버깅용)

    @Column(length = 50)
    private String aggregateId;      // 예: paymentId

    @Column(nullable = false, length = 1000)
    private String payload;          // 이벤트 본문(JSON) — 학습용이라 작은 페이로드, varchar로 충분

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    private LocalDateTime publishedAt;

    @Column(length = 255)
    private String lastError;

    private OutboxEvent(String eventType, String aggregateType, String aggregateId, String payload) {
        this.eventType = eventType;
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
    }

    /** 발행 대기 이벤트 생성(PENDING). 호출자의 트랜잭션에서 INSERT된다. */
    public static OutboxEvent pending(String eventType, String aggregateType, String aggregateId, String payload) {
        return new OutboxEvent(eventType, aggregateType, aggregateId, payload);
    }

    /** 발행 성공 → PUBLISHED. */
    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    /**
     * 발행 실패 기록 — 재시도 횟수를 늘리고, 최대치를 넘으면 FAILED(데드레터)로 보낸다.
     * (FAILED가 아니면 PENDING으로 남아 다음 폴링에 재시도된다.)
     */
    public void recordFailure(String error, int maxRetries) {
        this.retryCount++;
        this.lastError = (error != null && error.length() > 255) ? error.substring(0, 255) : error;
        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
        }
    }
}
