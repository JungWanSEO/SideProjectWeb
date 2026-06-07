package com.commerce.api.order.entity;

/**
 * 주문 상태.
 */
public enum OrderStatus {
    PENDING,    // 결제 대기 (주문 생성됨, 재고 미차감)
    PAID,       // 결제 완료 (재고 차감됨)
    CANCELLED   // 취소
}