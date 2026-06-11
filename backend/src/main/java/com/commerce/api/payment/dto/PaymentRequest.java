package com.commerce.api.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 요청.
 *
 * <p>{@code idempotencyKey}는 클라이언트가 생성하는 UUID — 같은 키의 재요청은 중복 결제되지 않고
 * 첫 요청의 결과를 그대로 돌려준다(네트워크 재시도·더블클릭 방어).
 */
public record PaymentRequest(

        @Schema(description = "결제할 주문 ID", example = "1")
        @NotNull(message = "orderId는 필수입니다.")
        Long orderId,

        @Schema(description = "멱등키(클라이언트가 생성하는 UUID) — 중복 결제 방지", example = "9f1c0c2e-7b3a-4d12-bf0a-1e2d3c4b5a6f")
        @NotBlank(message = "idempotencyKey는 필수입니다.")
        String idempotencyKey,

        @Schema(description = "결제수단(선택, 기본 MOCK_CARD)", example = "MOCK_CARD")
        String method,

        @Schema(description = "결제 PG(선택, 기본 TOSS). 지원: TOSS, KAKAOPAY. \"AUTO\"면 가장 싼 PG로 자동 라우팅(비용기반).",
                example = "TOSS")
        String provider
) {}
