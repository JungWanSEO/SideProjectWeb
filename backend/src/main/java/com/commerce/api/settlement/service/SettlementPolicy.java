package com.commerce.api.settlement.service;

/**
 * 정산 정책 — 입금 지연·수수료 계산 등 "비즈니스 규칙"을 모은다.
 *
 * <p><b>수수료율의 출처는 더 이상 여기가 아니다(MPG 비용기반).</b> 요율은 PG 고유 속성이라
 * {@code PaymentGateway.feeRate()}가 단일 출처고, 정산은 {@code PaymentGatewayRouter.feeRateOf(provider)}로
 * 읽는다(settlement → payment 정방향). 이렇게 해야 <b>라우팅 비용과 정산 수수료가 한 곳에서 정의</b>돼 어긋나지 않는다.
 * 이 클래스는 "요율로 수수료 금액을 떨어뜨리는 계산"과 입금 지연만 담당한다.
 */
public final class SettlementPolicy {

    /** 결제 승인 → 실제 입금까지의 지연 (모의: T+2일). 실무에선 영업일·정산주기에 따라 달라진다. */
    public static final int PAYOUT_DELAY_DAYS = 2;

    private SettlementPolicy() {
    }

    /** 수수료 = 결제액 × 수수료율 (원 단위 반올림). 요율은 호출자가 PG에서 읽어 넘긴다. KRW는 정수라 long으로. */
    public static long calculateFee(double feeRate, long grossAmount) {
        return Math.round(grossAmount * feeRate);
    }
}
