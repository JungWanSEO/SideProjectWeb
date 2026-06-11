package com.commerce.api.payment.gateway;

import org.springframework.stereotype.Component;

/**
 * 카카오페이 모의 PG 어댑터. 토스와 식별자·프리픽스만 다르고 동작은 공통 베이스를 그대로 쓴다.
 */
@Component
public class KakaoPayMockGateway extends AbstractMockPaymentGateway {

    @Override
    public String provider() {
        return "KAKAOPAY";
    }

    @Override
    public double feeRate() {
        return 0.028;   // 카카오페이 2.8%
    }

    @Override
    protected String idPrefix() {
        return "KAKAO";
    }
}
