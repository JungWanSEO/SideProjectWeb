package com.commerce.api.cart.dto;

import java.util.List;

/**
 * 장바구니 응답.
 * 항목의 상품명/가격/사이즈/재고는 스냅샷이 아니라 **조회 시점의 현재 상품·옵션 정보**로 채운다 (service에서 enrich).
 */
public record CartResponse(
        Long memberId,
        List<CartItemResponse> items,
        int totalQuantity
) {
    /** 장바구니 항목 응답. size·stock·soldOut은 라이브(현재) 옵션 정보. */
    public record CartItemResponse(
            Long productId,
            Long optionId,
            String productName,
            String size,
            long price,
            int quantity,
            long subtotal,
            int stock,
            boolean soldOut
    ) {
    }
}