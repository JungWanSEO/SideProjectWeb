package com.commerce.api.settlement.dto;

import com.commerce.api.settlement.entity.SettlementEntry;
import com.commerce.api.settlement.entity.SettlementStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 정산 항목 응답. */
public record SettlementResponse(
        Long id,
        Long paymentId,
        Long orderId,
        String pgTransactionId,
        long grossAmount,        // 결제액
        long fee,                // 수수료
        long netAmount,          // 실입금 (= grossAmount - fee)
        SettlementStatus status,
        LocalDate settledDate,
        LocalDateTime createdAt
) {
    public static SettlementResponse from(SettlementEntry entry) {
        return new SettlementResponse(
                entry.getId(),
                entry.getPaymentId(),
                entry.getOrderId(),
                entry.getPgTransactionId(),
                entry.getGrossAmount(),
                entry.getFee(),
                entry.getNetAmount(),
                entry.getStatus(),
                entry.getSettledDate(),
                entry.getCreatedAt()
        );
    }
}
