package com.commerce.api.payment.gateway;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/**
 * 모의 PG 어댑터 — 외부 호출 없이 결제 승인/환불을 시뮬레이션한다.
 *
 * 운영 전환 시 같은 {@link PaymentGateway} 인터페이스에 실제 PG(토스/포트원) 어댑터를 구현해
 * 교체하면 된다 — 서비스는 인터페이스에만 의존하므로 코드 변경이 없다.
 *
 * <p><b>자체 원장(ledger)을 보유한다.</b> 실제 PG가 자신이 처리한 거래를 기록하듯, 승인/환불을
 * 내부 맵에 남긴다 — 이게 {@link #fetchSettlements()}가 돌려주는 "PG 측 진실". 대사(reconciliation)가
 * 우리 DB와 독립된 이 원장을 대조하므로 불일치가 의미를 가진다(같은 출처를 베끼면 대사할 게 없다).
 *
 * <p>⚠️ 원장은 <b>인메모리</b>라 앱 재시작 시 사라진다(단일 세션 검증용 모의). 실제 PG는 PG 쪽에
 * 영속된다. 또 싱글톤 빈이 동시 요청을 받으므로 {@link ConcurrentHashMap}으로 둔다.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private final ConcurrentMap<String, PgSettlementRecord> ledger = new ConcurrentHashMap<>();

    @Override
    public PaymentApproval approve(PaymentApprovalCommand command) {
        // 테스트용 실패 트리거: 금액이 0 이하이면 실패(정상 주문 흐름에서는 발생하지 않음).
        if (command.amount() <= 0) {
            return PaymentApproval.failed("유효하지 않은 결제 금액: " + command.amount());
        }
        // 실제 PG라면 외부 API를 호출해 거래 ID를 받지만, 모의에서는 가짜 ID를 생성한다.
        String pgTransactionId = "MOCK-" + UUID.randomUUID();
        // 승인 거래를 원장에 기록(PG 관점 = PAID) → 나중에 대사가 우리 기록과 대조한다.
        ledger.put(pgTransactionId, new PgSettlementRecord(pgTransactionId, command.amount(), PgSettlementStatus.PAID));
        return PaymentApproval.approved(pgTransactionId);
    }

    @Override
    public PaymentRefund refund(PaymentRefundCommand command) {
        // 원거래(command.pgTransactionId)를 원장에서 REFUNDED로 갱신 → 대사에서 '정산 후 환불'이
        // 우리 정산 기록(환불 미반영)과 어긋나 STATUS_MISMATCH로 잡힌다.
        ledger.computeIfPresent(command.pgTransactionId(),
                (id, rec) -> new PgSettlementRecord(id, rec.amount(), PgSettlementStatus.REFUNDED));
        String pgRefundId = "MOCK-REFUND-" + UUID.randomUUID();
        return PaymentRefund.refunded(pgRefundId);
    }

    @Override
    public List<PgSettlementRecord> fetchSettlements() {
        // PG가 처리한 거래 전체(원장 스냅샷)를 정산 리포트로 돌려준다.
        return List.copyOf(ledger.values());
    }
}
