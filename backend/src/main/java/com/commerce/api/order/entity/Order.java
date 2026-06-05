package com.commerce.api.order.entity;

import com.commerce.api.global.common.BaseEntity;
import com.commerce.api.global.exception.BusinessException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 주문 (애그리거트 루트).
 *
 * - 테이블명은 "orders" (ORDER는 SQL 예약어).
 * - 회원은 ID 참조(memberId, 다른 애그리거트). 주문 항목은 애그리거트 내부 → @OneToMany 객체 연관.
 * - totalPrice는 항목 추가 시 누적 계산 → 애그리거트가 스스로 정합성을 유지.
 */
@Getter
@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;   // 다른 애그리거트(회원) → ID 참조

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private long totalPrice;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    private Order(Long memberId) {
        this.memberId = memberId;
        this.status = OrderStatus.ORDERED;
        this.totalPrice = 0L;
    }

    /** 빈 주문 생성 (항목은 addItem으로 추가) */
    public static Order create(Long memberId) {
        return new Order(memberId);
    }

    /** 주문 항목 추가 + 양방향 연관 설정 + 총액 누적 */
    public void addItem(OrderItem item) {
        orderItems.add(item);
        item.assignOrder(this);
        this.totalPrice += item.getSubtotal();
    }

    /** 주문 취소 (이미 취소된 주문은 불가) */
    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 취소된 주문입니다.");
        }
        this.status = OrderStatus.CANCELLED;
    }
}