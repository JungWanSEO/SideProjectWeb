package com.commerce.api.payment.gateway;

/**
 * PG 환불(취소) 결과.
 *
 * - refunded=true  → pgRefundId 보유 (failureReason=null)
 * - refunded=false → failureReason 보유 (pgRefundId=null)
 *
 * 승인 결과({@link PaymentApproval})와 같은 모양 — 실패는 예외가 아니라 결과로 표현한다(정상 분기).
 */
public record PaymentRefund(boolean refunded, String pgRefundId, String failureReason) {

    public static PaymentRefund refunded(String pgRefundId) {
        return new PaymentRefund(true, pgRefundId, null);
    }

    public static PaymentRefund failed(String failureReason) {
        return new PaymentRefund(false, null, failureReason);
    }
}
