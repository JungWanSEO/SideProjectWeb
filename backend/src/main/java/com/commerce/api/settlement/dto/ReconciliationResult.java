package com.commerce.api.settlement.dto;

/**
 * 대사 실행 결과 요약 — "오늘 대사 결과 한 장".
 *
 * <p>일치 건수 + 유형별 불일치 건수. 0 불일치면 양측이 완전히 맞은 것.
 */
public record ReconciliationResult(
        int matched,           // 양측 존재·금액·상태 일치
        int missingInPg,       // 우리엔 있고 PG엔 없음
        int missingInOurs,     // PG엔 있고 우리엔 없음
        int amountMismatch,    // 양측 있으나 금액 상이
        int statusMismatch,    // 우리 정산 ↔ PG 환불 등 상태 상이
        int totalMismatches    // 불일치 합계(= 위 4개 합)
) {
}
