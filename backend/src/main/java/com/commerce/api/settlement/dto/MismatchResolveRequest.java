package com.commerce.api.settlement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * 불일치 처리(resolve/ignore) 요청 — 처리 사유(선택).
 * 본문 없이 호출해도 되며, 그 경우 사유는 비어 있다.
 */
@Schema(description = "불일치 처리 요청")
public record MismatchResolveRequest(

        @Schema(description = "처리 사유(선택)", example = "PG 정산 후 환불분 수기 상계 처리")
        @Size(max = 255, message = "사유는 255자 이하여야 합니다.")
        String note
) {
}
