package com.commerce.api.payment.gateway;

import java.util.List;

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

    /**
     * PG에 결제 취소(환불)를 요청한다.
     * 승인과 마찬가지로 실패는 예외가 아니라 {@link PaymentRefund} 결과로 표현한다.
     */
    PaymentRefund refund(PaymentRefundCommand command);

    /**
     * PG 정산 리포트를 조회한다 — 대사(reconciliation)에서 우리 기록과 대조할 PG 측 진실의 출처.
     * 실제 PG는 일자별 정산 파일/리포트를 제공하지만, 모의 단계에선 PG가 처리한 거래 전체를 돌려준다.
     */
    List<PgSettlementRecord> fetchSettlements();

    /** 승인 요청 입력. */
    record PaymentApprovalCommand(Long orderId, long amount, String idempotencyKey) {}

    /** 환불 요청 입력. pgTransactionId = 승인 때 받은 원거래 ID(이 거래를 취소). */
    record PaymentRefundCommand(Long orderId, long amount, String pgTransactionId) {}
}
