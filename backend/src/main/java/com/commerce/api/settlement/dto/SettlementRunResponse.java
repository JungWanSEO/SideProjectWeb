package com.commerce.api.settlement.dto;

/**
 * 정산 배치 실행 결과 요약.
 *
 * <p>이번 실행으로 새로 만든 정산 항목 수와 합계(매출/수수료/실입금)를 돌려준다 —
 * "오늘 정산 100건, 결제액 합 1,000,000 / 수수료 25,000 / 실입금 975,000" 같은 한 줄 요약.
 */
public record SettlementRunResponse(
        int createdCount,        // 이번 실행으로 새로 만든 정산 항목 수
        long totalGrossAmount,   // 결제액 합계
        long totalFee,           // 수수료 합계
        long totalNetAmount      // 실입금 합계 (= 결제액 - 수수료)
) {
}
