package com.commerce.api.product.entity;

/**
 * 상품 상태.
 * 물리 삭제 대신 상태로 관리한다 (주문이 상품을 참조하므로).
 */
public enum ProductStatus {
    ON_SALE,       // 판매중
    SOLD_OUT,      // 품절
    DISCONTINUED   // 판매중지
}