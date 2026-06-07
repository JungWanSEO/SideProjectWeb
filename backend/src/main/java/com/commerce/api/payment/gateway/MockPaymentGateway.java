package com.commerce.api.payment.gateway;

import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 모의 PG 어댑터 — 외부 호출 없이 결제 승인을 시뮬레이션한다.
 *
 * 운영 전환 시 같은 {@link PaymentGateway} 인터페이스에 실제 PG(토스/포트원) 어댑터를 구현해
 * 교체하면 된다 — 서비스는 인터페이스에만 의존하므로 코드 변경이 없다.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    @Override
    public PaymentApproval approve(PaymentApprovalCommand command) {
        // 테스트용 실패 트리거: 금액이 0 이하이면 실패(정상 주문 흐름에서는 발생하지 않음).
        if (command.amount() <= 0) {
            return PaymentApproval.failed("유효하지 않은 결제 금액: " + command.amount());
        }
        // 실제 PG라면 외부 API를 호출해 거래 ID를 받지만, 모의에서는 가짜 ID를 생성한다.
        String pgTransactionId = "MOCK-" + UUID.randomUUID();
        return PaymentApproval.approved(pgTransactionId);
    }

    @Override
    public PaymentRefund refund(PaymentRefundCommand command) {
        // 모의 PG는 환불을 항상 성공으로 처리하고 가짜 환불 거래 ID를 발급한다.
        // (실제 PG라면 원거래 ID(command.pgTransactionId)로 취소 API를 호출한다.)
        String pgRefundId = "MOCK-REFUND-" + UUID.randomUUID();
        return PaymentRefund.refunded(pgRefundId);
    }
}
