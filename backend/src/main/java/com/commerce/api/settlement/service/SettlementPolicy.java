package com.commerce.api.settlement.service;

/**
 * 정산 정책 — 수수료율·입금 지연 등 "비즈니스 규칙"을 한 곳에 모은다.
 *
 * <p>모의 단계라 고정 상수로 두지만, 실무에서 수수료는 <b>결제수단·카드사·상품·계약</b>마다 달라
 * 보통 설정값이나 별도 테이블(요율표)로 분리한다. 정산 스키마가 회사마다 다른 근본 이유가 이것 —
 * 정산은 그 회사 고유의 비즈니스 도메인이다(docs/payment-modern-architecture.md §3.6).
 *
 * <p>.NET으로 치면 {@code appsettings.json}의 요율 섹션 또는 정책 테이블을 읽어오는 자리.
 */
public final class SettlementPolicy {

    /** PG 수수료율 (모의: 2.5% 고정). */
    public static final double FEE_RATE = 0.025;

    /** 결제 승인 → 실제 입금까지의 지연 (모의: T+2일). 실무에선 영업일·정산주기에 따라 달라진다. */
    public static final int PAYOUT_DELAY_DAYS = 2;

    private SettlementPolicy() {
    }

    /** 수수료 = 결제액 × 수수료율 (원 단위 반올림). KRW는 정수라 long으로 떨어뜨린다. */
    public static long calculateFee(long grossAmount) {
        return Math.round(grossAmount * FEE_RATE);
    }
}
