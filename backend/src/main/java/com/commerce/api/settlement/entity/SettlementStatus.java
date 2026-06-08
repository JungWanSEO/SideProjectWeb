package com.commerce.api.settlement.entity;

/**
 * 정산 항목 상태.
 *
 * <p>결제(Payment)의 상태머신과 별개다 — 결제는 "거래가 승인됐나", 정산은 "며칠 뒤 수수료 떼고
 * 실제로 입금됐나"를 추적한다(다른 시점·다른 관심사 → 다른 도메인).
 *
 * <pre>
 *   SCHEDULED  정산 예정 (배치가 PAID 결제를 잡아 만든 직후. 입금일 = T+N)
 *      └─▶ PAID_OUT  입금 완료 (가맹점 계좌로 실입금 확인)
 * </pre>
 *
 * ⚠️ enum 값 순서는 알파벳순(PAID_OUT, SCHEDULED)으로 둔다 — Hibernate가 @Enumerated(STRING)을
 * MySQL 네이티브 ENUM으로 매핑하므로, 손수 쓰는 Flyway 마이그레이션의 enum 값 집합과 순서가
 * 일치해야 {@code ddl-auto: validate}를 통과한다(V2 OAuth 마이그레이션에서 얻은 교훈).
 */
public enum SettlementStatus {
    PAID_OUT,
    SCHEDULED
}
