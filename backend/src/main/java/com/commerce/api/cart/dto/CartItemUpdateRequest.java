package com.commerce.api.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 장바구니 항목 수량 변경 요청.
 * 담기(CartItemAddRequest)가 수량을 "더한다"면, 이건 수량을 "절대값으로 설정"한다(스테퍼/수량 입력용).
 */
@Schema(description = "장바구니 항목 수량 변경 요청")
public record CartItemUpdateRequest(

        @Schema(description = "변경할 수량(1 이상). 기존 수량을 덮어쓴다.", example = "3")
        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 1 이상이어야 합니다.")
        Integer quantity
) {
}
