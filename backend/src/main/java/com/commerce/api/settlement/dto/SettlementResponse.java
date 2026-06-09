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
        String provider,         // 정산 대상 결제를 처리한 PG (MPG-3)
        long grossAmount,        // 결제액
        long fee,                // 수수료
        double feeRate,          // 적용한 수수료율 스냅샷 (예: 0.025)
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
                entry.getProvider(),
                entry.getGrossAmount(),
                entry.getFee(),
                entry.getFeeRate(),
                entry.getNetAmount(),
                entry.getStatus(),
                entry.getSettledDate(),
                entry.getCreatedAt()
        );
    }
}
