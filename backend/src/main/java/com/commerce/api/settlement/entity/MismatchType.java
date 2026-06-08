package com.commerce.api.settlement.entity;

/**
 * 대사(reconciliation) 불일치 유형.
 *
 * <p>우리 {@code SettlementEntry}와 PG 정산 리포트를 {@code pgTransactionId}로 매칭한 뒤,
 * 어긋나는 건을 이 유형으로 분류한다. (일치 건 MATCHED는 별도로 저장하지 않고 요약 카운트만.)
 *
 * <pre>
 *   MISSING_IN_PG    우리엔 있는데 PG 리포트엔 없음   → 웹훅 유실/PG 누락 의심
 *   MISSING_IN_OURS  PG엔 있는데 우리 기록 없음        → 우리 누락(정산 미실행 등)
 *   AMOUNT_MISMATCH  양쪽 있으나 금액 상이             → 수수료·부분취소 반영 차이
 *   STATUS_MISMATCH  우리는 정산했는데 PG는 환불됨      → 정산 후 취소분(상계 필요)
 * </pre>
 *
 * ⚠️ enum 값은 알파벳순 — Hibernate ENUM DDL ↔ Flyway 마이그레이션 일치(validate).
 */
public enum MismatchType {
    AMOUNT_MISMATCH,
    MISSING_IN_OURS,
    MISSING_IN_PG,
    STATUS_MISMATCH
}
