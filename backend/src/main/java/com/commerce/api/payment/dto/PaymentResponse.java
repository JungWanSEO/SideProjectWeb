package com.commerce.api.payment.dto;

import com.commerce.api.payment.entity.Payment;
import com.commerce.api.payment.entity.PaymentStatus;
import java.time.LocalDateTime;

/** 결제 응답. */
public record PaymentResponse(
        Long id,
        Long orderId,
        long amount,
        PaymentStatus status,
        String method,
        String provider,          // 결제를 처리한 PG (예: TOSS, KAKAOPAY)
        String pgTransactionId,   // 승인 성공 시에만 채워짐
        LocalDateTime createdAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getMethod(),
                payment.getProvider(),
                payment.getPgTransactionId(),
                payment.getCreatedAt()
        );
    }
}
