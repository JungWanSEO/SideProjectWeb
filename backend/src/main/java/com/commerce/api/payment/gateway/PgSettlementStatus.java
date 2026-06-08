package com.commerce.api.payment.gateway;

/**
 * PG 정산 리포트상의 거래 상태.
 *
 * <p>PG가 "이 거래가 결제됐다/환불됐다"고 통보하는 관점 — 우리 내부 상태머신과 별개다.
 * 대사(reconciliation)는 이 PG 관점과 우리 기록을 대조한다.
 */
public enum PgSettlementStatus {
    PAID,
    REFUNDED
}
