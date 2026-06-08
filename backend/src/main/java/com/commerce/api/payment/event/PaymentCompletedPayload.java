package com.commerce.api.payment.event;

/**
 * 결제 완료(PAYMENT_COMPLETED) 이벤트 페이로드 — 아웃박스에 JSON으로 직렬화되는 이벤트 계약.
 * 소비자(알림 등)는 이 계약으로 역직렬화한다.
 */
public record PaymentCompletedPayload(Long orderId, Long paymentId, long amount) {
}
