package com.commerce.api.payment.gateway;

/**
 * PG 정산 리포트의 한 줄 — "PG가 통보한 거래".
 *
 * <p>대사(reconciliation)에서 우리 {@code SettlementEntry}와 {@code pgTransactionId}로 매칭하는
 * 대상이다. PG가 진실의 한 출처고, 우리 기록이 다른 출처 — 둘을 대조해 불일치를 찾는다.
 *
 * @param pgTransactionId 거래 식별자(조인 키 — 우리 결제의 pgTransactionId와 같은 값)
 * @param amount          PG가 보고한 거래 금액(원)
 * @param status          PG 관점의 상태(PAID/REFUNDED)
 */
public record PgSettlementRecord(String pgTransactionId, long amount, PgSettlementStatus status) {
}
