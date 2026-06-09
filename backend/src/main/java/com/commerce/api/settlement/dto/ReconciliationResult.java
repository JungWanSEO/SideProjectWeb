package com.commerce.api.settlement.dto;

import java.util.List;

/**
 * 대사 실행 결과 요약 — "오늘 대사 결과 한 장".
 *
 * <p>일치 건수 + 유형별 <b>새 OPEN 불일치</b> 건수 + 이미 처리되어 건너뛴 건수.
 * 유형별 카운트는 이번에 새로 OPEN으로 기록한 것만 — RESOLVED/IGNORED로 처리된 거래키는
 * {@code alreadyHandled}로만 집계하고 다시 OPEN으로 만들지 않는다.
 *
 * <p><b>MPG-2:</b> {@code byProvider}로 PG별 분해도 함께 준다 — "토스는 깨끗(matched 10), 카카오는
 * 상태상이 2건"처럼 어느 PG에서 어긋남이 몰리는지 한눈에. (provider 알파벳순.)
 */
public record ReconciliationResult(
        int matched,           // 양측 존재·금액·상태 일치
        int missingInPg,       // 우리엔 있고 PG엔 없음
        int missingInOurs,     // PG엔 있고 우리엔 없음
        int amountMismatch,    // 양측 있으나 금액 상이
        int statusMismatch,    // 우리 정산 ↔ PG 환불 등 상태 상이
        int totalMismatches,   // 새 OPEN 불일치 합계(= 위 4개 합)
        int alreadyHandled,    // 이미 RESOLVED/IGNORED라 건너뛴 거래키 수
        List<ProviderReconciliation> byProvider   // PG별 분해
) {

    /** PG 한 곳의 대사 결과 — 같은 분류를 그 PG에 한정해 집계. */
    public record ProviderReconciliation(
            String provider,       // PG 코드 (예: TOSS, KAKAOPAY)
            int matched,
            int missingInPg,
            int missingInOurs,
            int amountMismatch,
            int statusMismatch,
            int totalMismatches,
            int alreadyHandled
    ) {
    }
}
