package com.commerce.api.order.repository;

import com.commerce.api.order.entity.Order;
import com.commerce.api.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 주문 DB 접근.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /** 특정 회원의 주문을 페이지로 조회 (정렬·페이지 크기는 Pageable에 따름). */
    Page<Order> findByMemberId(Long memberId, Pageable pageable);

    /**
     * 이 회원이 해당 상품을 특정 상태(예: PAID)로 주문한 적이 있는지 — 리뷰 "구매자만 작성" 검증용.
     * 주문 항목(orderItems) 컬렉션을 조인해 productId를 본다(파생 쿼리의 _ 는 연관 경로 탐색 표시).
     */
    boolean existsByMemberIdAndStatusAndOrderItems_ProductId(
            Long memberId, OrderStatus status, Long productId);
}