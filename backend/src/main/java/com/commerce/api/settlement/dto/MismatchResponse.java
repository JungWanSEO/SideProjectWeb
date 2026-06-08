package com.commerce.api.settlement.dto;

import com.commerce.api.settlement.entity.Mismatch;
import com.commerce.api.settlement.entity.MismatchType;
import java.time.LocalDateTime;

/** 대사 불일치 항목 응답. */
public record MismatchResponse(
        Long id,
        String pgTransactionId,
        MismatchType type,
        Long ourAmount,      // 한쪽에만 있으면 null
        Long pgAmount,       // 한쪽에만 있으면 null
        String detail,
        LocalDateTime createdAt
) {
    public static MismatchResponse from(Mismatch m) {
        return new MismatchResponse(
                m.getId(),
                m.getPgTransactionId(),
                m.getType(),
                m.getOurAmount(),
                m.getPgAmount(),
                m.getDetail(),
                m.getCreatedAt()
        );
    }
}
