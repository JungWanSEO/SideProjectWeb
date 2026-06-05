package com.commerce.api.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 장바구니 담기 요청.
 */
@Schema(description = "장바구니 담기 요청")
public record CartItemAddRequest(

        @Schema(description = "옵션(사이즈) ID", example = "1")
        @NotNull(message = "옵션 ID는 필수입니다.")
        Long optionId,

        @Schema(description = "수량(1 이상)", example = "2")
        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 1 이상이어야 합니다.")
        Integer quantity
) {
}