package com.commerce.api.payment.gateway;

import org.springframework.stereotype.Component;

/**
 * 토스페이먼츠 모의 PG 어댑터. 실제 연동 시 이 클래스만 실제 토스 SDK 호출로 교체하면 된다(포트는 불변).
 */
@Component
public class TossMockGateway extends AbstractMockPaymentGateway {

    @Override
    public String provider() {
        return "TOSS";
    }

    @Override
    public double feeRate() {
        return 0.025;   // 토스페이먼츠 2.5%
    }

    @Override
    protected String idPrefix() {
        return "TOSS";
    }
}
