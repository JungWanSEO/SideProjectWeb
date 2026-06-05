package com.commerce.api.cart.entity;

import com.commerce.api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 장바구니 항목 (Cart 애그리거트 내부).
 * 상품·옵션은 ID 참조(productId·optionId). 가격/이름 스냅샷 없음 — 조회 시 현재 상품 정보를 따른다.
 *
 * <p>식별 단위는 <b>옵션(사이즈)</b>이다 — 같은 상품이라도 사이즈가 다르면 별개 항목.
 * 주문의 OrderItem과 동형(productId + optionId를 함께 보유)이라 장바구니→주문 전환이 자연스럽다.
 */
@Getter
@Entity
@Table(name = "cart_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CartItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(nullable = false)
    private Long productId;   // 다른 애그리거트(상품) → ID 참조 (enrich·표시용)

    @Column(nullable = false)
    private Long optionId;    // 상품 옵션(사이즈) → ID 참조. 장바구니 항목의 식별 단위.

    @Column(nullable = false)
    private int quantity;

    @Builder
    private CartItem(Long productId, Long optionId, int quantity) {
        this.productId = productId;
        this.optionId = optionId;
        this.quantity = quantity;
    }

    void assignCart(Cart cart) {
        this.cart = cart;
    }

    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }
}