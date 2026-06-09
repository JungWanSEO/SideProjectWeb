package com.commerce.api.settlement.dto;

import java.util.List;

/**
 * 정산 배치 실행 결과 요약.
 *
 * <p>이번 실행으로 새로 만든 정산 항목 수와 합계(매출/수수료/실입금)를 돌려준다 —
 * "오늘 정산 100건, 결제액 합 1,000,000 / 수수료 25,000 / 실입금 975,000" 같은 한 줄 요약.
 *
 * <p><b>MPG-3:</b> {@code byProvider}로 PG별 분해도 함께 준다. 같은 결제액이라도 PG 요율이 달라
 * 수수료·실입금이 갈리는 걸 한눈에 — "TOSS 2건 2.5% / KAKAOPAY 1건 2.8%"처럼.
 */
public record SettlementRunResponse(
        int createdCount,        // 이번 실행으로 새로 만든 정산 항목 수
        long totalGrossAmount,   // 결제액 합계
        long totalFee,           // 수수료 합계
        long totalNetAmount,     // 실입금 합계 (= 결제액 - 수수료)
        List<ProviderBreakdown> byProvider   // PG별 분해 (요율 차이가 수수료 차이를 만드는 걸 보여줌)
) {

    /** PG 한 곳의 정산 분해 — 요율·건수·금액 합계. */
    public record ProviderBreakdown(
            String provider,         // PG 코드 (예: TOSS, KAKAOPAY)
            double feeRate,          // 그 PG에 적용한 수수료율 (예: 0.025)
            int count,               // 이 PG로 만든 정산 항목 수
            long grossAmount,        // 결제액 합계
            long fee,                // 수수료 합계
            long netAmount           // 실입금 합계
    ) {
    }
}
