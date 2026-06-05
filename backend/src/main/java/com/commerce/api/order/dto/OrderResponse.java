package com.commerce.api.order.dto;

import com.commerce.api.order.entity.Order;
import com.commerce.api.order.entity.OrderItem;
import com.commerce.api.order.entity.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 응답.
 */
public record OrderResponse(
        Long id,
        Long memberId,
        OrderStatus status,
        long totalPrice,
        List<OrderItemResponse> items,
        LocalDateTime createdAt
) {
    public static OrderResponse from(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getMemberId(),
                order.getStatus(),
                order.getTotalPrice(),
                items,
                order.getCreatedAt()
        );
    }

    /** 주문 항목 응답 (스냅샷된 상품명·사이즈·가격 + 소계) */
    public record OrderItemResponse(
            Long productId,
            Long optionId,
            String productName,
            String size,
            long orderPrice,
            int quantity,
            long subtotal
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getProductId(),
                    item.getOptionId(),
                    item.getProductName(),
                    item.getSize(),
                    item.getOrderPrice(),
                    item.getQuantity(),
                    item.getSubtotal()
            );
        }
    }
}