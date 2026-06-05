package com.commerce.api.product.dto;

import com.commerce.api.product.entity.ProductOption;

/**
 * 상품 옵션(사이즈) 응답. {@code soldOut}으로 사이즈별 품절을 표시한다.
 */
public record ProductOptionResponse(Long id, String size, int stock, boolean soldOut) {

    public static ProductOptionResponse from(ProductOption option) {
        return new ProductOptionResponse(
                option.getId(), option.getSize(), option.getStock(), option.isSoldOut());
    }
}
