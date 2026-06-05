package com.commerce.api.order.service;

import com.commerce.api.cart.entity.Cart;
import com.commerce.api.cart.repository.CartRepository;
import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.order.dto.OrderCreateRequest;
import com.commerce.api.order.dto.OrderCreateRequest.OrderItemRequest;
import com.commerce.api.order.dto.OrderResponse;
import com.commerce.api.order.entity.Order;
import com.commerce.api.order.entity.OrderItem;
import com.commerce.api.order.repository.OrderRepository;
import com.commerce.api.product.entity.Product;
import com.commerce.api.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 처리 트랜잭션 워커.
 *
 * OrderService.create/checkout가 @Retryable로 이 메서드를 호출한다. 재시도마다 새 트랜잭션으로 실행되도록
 * 트랜잭션 경계를 OrderService(재시도)와 분리한다 — 낙관적 락 충돌은 커밋 시점에 발생하므로
 * 같은 트랜잭션 안에서 재시도할 수 없기 때문.
 */
@Service
@RequiredArgsConstructor
public class OrderProcessor {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;

    /** 명시적 항목 목록으로 주문 생성 (POST /api/orders). */
    @Transactional
    public OrderResponse place(Long memberId, OrderCreateRequest request) {
        return placeOrder(memberId, request.items());
    }

    /**
     * 체크아웃: 서버의 장바구니를 읽어 그대로 주문 생성 + 장바구니 비우기 (한 트랜잭션).
     * 클라이언트는 항목을 보내지 않는다 — <b>서버 장바구니가 진실의 원천</b>(위변조 방지).
     * 주문 생성·재고 차감·장바구니 비우기가 원자적 → 부분 실패(주문됐는데 장바구니 잔존) 없음.
     */
    @Transactional
    public OrderResponse checkout(Long memberId) {
        Cart cart = cartRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다."));

        List<OrderItemRequest> items = cart.getCartItems().stream()
                .map(ci -> new OrderItemRequest(ci.getOptionId(), ci.getQuantity()))
                .toList();
        if (items.isEmpty()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "장바구니가 비어 있습니다.");
        }

        OrderResponse response = placeOrder(memberId, items);
        cart.clearItems();   // 주문 성공 후 장바구니 비우기 (orphanRemoval로 DB 삭제, 같은 트랜잭션)
        return response;
    }

    /**
     * 항목마다 상품 조회 → 재고 차감(@Version 낙관적 락) → 주문 시점 스냅샷 → 추가.
     * 동시 차감 충돌 시 커밋에서 OptimisticLockingFailureException 발생(상위에서 재시도).
     * 하나라도 실패하면 트랜잭션 전체 롤백(재고 차감도 되돌아감).
     */
    private OrderResponse placeOrder(Long memberId, List<OrderItemRequest> items) {
        Order order = Order.create(memberId);

        for (OrderItemRequest itemRequest : items) {
            // 루트 경유: 옵션 ID로 Product 애그리거트를 로드(이름·가격 스냅샷에 어차피 필요)
            Product product = productRepository.findByOptionId(itemRequest.optionId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "옵션을 찾을 수 없습니다. (id: " + itemRequest.optionId() + ")"));

            // 해당 옵션 재고 차감 (부족 시 409 → 롤백, 동시 차감 충돌은 옵션 @Version으로 감지)
            product.decreaseStock(itemRequest.optionId(), itemRequest.quantity());

            OrderItem orderItem = OrderItem.builder()
                    .productId(product.getId())
                    .optionId(itemRequest.optionId())
                    .productName(product.getName())                       // 스냅샷
                    .size(product.optionSize(itemRequest.optionId()))     // 사이즈 스냅샷
                    .orderPrice(product.getPrice())                       // 스냅샷
                    .quantity(itemRequest.quantity())
                    .build();
            order.addItem(orderItem);
        }

        return OrderResponse.from(orderRepository.save(order));
    }
}
