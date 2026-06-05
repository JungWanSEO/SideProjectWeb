package com.commerce.api.payment.gateway;

/**
 * 결제 게이트웨이(PG) 포트 — 외부 결제 연동의 추상화(포트-어댑터).
 *
 * 서비스는 이 인터페이스에만 의존하고, 구현체(어댑터)를 갈아끼워 모의/실제 PG를 교체한다(DIP).
 * .NET에서 {@code IPaymentGateway}를 DI로 주입하고 Mock/실제 구현을 바꾸는 것과 동형.
 */
public interface PaymentGateway {

    /**
     * PG에 결제 승인을 요청한다.
     * 승인 실패는 예외가 아니라 {@link PaymentApproval} 결과로 표현한다(승인 여부는 정상 분기).
     */
    PaymentApproval approve(PaymentApprovalCommand command);

    /** 승인 요청 입력. */
    record PaymentApprovalCommand(Long orderId, long amount, String idempotencyKey) {}
}
