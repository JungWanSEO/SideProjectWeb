package com.commerce.api.cart.repository;

import com.commerce.api.cart.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 장바구니 DB 접근.
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByMemberId(Long memberId);
}