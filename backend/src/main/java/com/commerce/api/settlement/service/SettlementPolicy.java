package com.commerce.api.settlement.service;

import java.util.Map;

/**
 * 정산 정책 — 수수료율·입금 지연 등 "비즈니스 규칙"을 한 곳에 모은다.
 *
 * <p><b>MPG-3: 수수료율은 PG(provider)마다 다르다.</b> 다중 PG(토스/카카오…)를 붙이고 나면
 * "매출 ≠ 결제액"의 그 차이가 PG 선택에 따라 달라진다 — 같은 10,000원이라도 토스로 받으면 수수료
 * 250원, 카카오로 받으면 280원처럼. 이 차이를 정산까지 끌고 와야 다중 PG가 실제 가치를 갖는다.
 *
 * <p>모의 단계라 요율표를 <b>상수 Map</b>으로 둔다. 실무에선 결제수단·카드사·상품·계약마다 달라
 * 보통 설정값(application.yml)이나 별도 테이블(요율표)로 분리한다 — 정산 스키마가 회사마다 다른
 * 근본 이유가 이것이다(docs/payment-modern-architecture.md §3.6).
 * .NET으로 치면 {@code appsettings.json}의 요율 섹션 또는 정책 테이블을 읽어오는 자리.
 */
public final class SettlementPolicy {

    /**
     * PG별 수수료율 (모의값). 키는 대문자 provider 코드(Payment.provider와 동일 표기).
     * 실무 요율은 계약·카드사·정산주기에 따라 더 잘게 나뉜다.
     */
    private static final Map<String, Double> FEE_RATES = Map.of(
            "TOSS", 0.025,        // 토스페이먼츠 2.5%
            "KAKAOPAY", 0.028     // 카카오페이 2.8%
    );

    /** 요율표에 없는 PG의 폴백 — 보수적으로 가장 높게(3.0%). 신규 PG가 요율 누락 시 과소청구를 막는다. */
    public static final double DEFAULT_FEE_RATE = 0.030;

    /** 결제 승인 → 실제 입금까지의 지연 (모의: T+2일). 실무에선 영업일·정산주기에 따라 달라진다. */
    public static final int PAYOUT_DELAY_DAYS = 2;

    private SettlementPolicy() {
    }

    /**
     * 해당 PG의 수수료율. provider가 null/blank거나 요율표에 없으면 {@link #DEFAULT_FEE_RATE}.
     * provider 표기는 라우터가 대문자로 정규화하지만, 안전하게 여기서도 대문자로 맞춘다.
     */
    public static double rateFor(String provider) {
        if (provider == null || provider.isBlank()) {
            return DEFAULT_FEE_RATE;
        }
        return FEE_RATES.getOrDefault(provider.toUpperCase(), DEFAULT_FEE_RATE);
    }

    /** 수수료 = 결제액 × 해당 PG 요율 (원 단위 반올림). KRW는 정수라 long으로 떨어뜨린다. */
    public static long calculateFee(String provider, long grossAmount) {
        return Math.round(grossAmount * rateFor(provider));
    }
}
