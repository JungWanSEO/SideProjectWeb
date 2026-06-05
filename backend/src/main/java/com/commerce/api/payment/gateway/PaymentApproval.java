package com.commerce.api.payment.gateway;

/**
 * PG 승인 결과.
 *
 * - approved=true  → pgTransactionId 보유 (failureReason=null)
 * - approved=false → failureReason 보유 (pgTransactionId=null)
 */
public record PaymentApproval(boolean approved, String pgTransactionId, String failureReason) {

    public static PaymentApproval approved(String pgTransactionId) {
        return new PaymentApproval(true, pgTransactionId, null);
    }

    public static PaymentApproval failed(String failureReason) {
        return new PaymentApproval(false, null, failureReason);
    }
}
