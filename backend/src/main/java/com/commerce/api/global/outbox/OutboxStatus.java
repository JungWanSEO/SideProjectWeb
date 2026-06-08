package com.commerce.api.global.outbox;

/**
 * 아웃박스 이벤트의 발행 상태.
 *
 * <pre>
 *   PENDING    미발행(쓰기 트랜잭션에서 INSERT된 직후)
 *    ├─▶ PUBLISHED  발행 완료(폴러가 핸들러까지 성공)
 *    └─▶ FAILED     최대 재시도 초과(데드레터 — 사람이 본다)
 * </pre>
 *
 * ⚠️ enum 값은 알파벳순(FAILED, PENDING, PUBLISHED) — Hibernate ENUM DDL ↔ Flyway 일치(validate).
 */
public enum OutboxStatus {
    FAILED,
    PENDING,
    PUBLISHED
}
