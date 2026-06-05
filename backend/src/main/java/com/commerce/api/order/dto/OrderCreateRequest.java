package com.commerce.api.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * 주문 생성 요청.
 * 주문자(memberId)는 요청에 받지 않고 인증된 로그인 사용자에서 가져온다.
 */
@Schema(description = "주문 생성 요청")
public record OrderCreateRequest(

        @Schema(description = "주문 항목 목록(최소 1개)")
        @NotEmpty(message = "주문 항목은 최소 1개 이상이어야 합니다.")
        @Valid
        List<OrderItemRequest> items
) {
    /** 주문 항목 요청 (옵션(사이즈) ID + 수량) */
    @Schema(description = "주문 항목")
    public record OrderItemRequest(

            @Schema(description = "옵션(사이즈) ID", example = "1")
            @NotNull(message = "옵션 ID는 필수입니다.")
            Long optionId,

            @Schema(description = "수량(1 이상)", example = "2")
            @NotNull(message = "수량은 필수입니다.")
            @Positive(message = "수량은 1 이상이어야 합니다.")
            Integer quantity
    ) {
    }
}
