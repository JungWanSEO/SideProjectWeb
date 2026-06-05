package com.commerce.api.cart.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.commerce.api.cart.entity.Cart;
import com.commerce.api.global.config.JpaConfig;
import com.commerce.api.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * CartRepository 슬라이스 테스트 (@DataJpaTest).
 */
@DataJpaTest
// @DataJpaTest는 프로젝트의 모든 리포지토리를 로드한다 → QueryDSL을 쓰는 ProductRepository가
// 필요로 하는 JPAQueryFactory 빈을 QuerydslConfig로 함께 제공해야 컨텍스트가 뜬다.
@Import({JpaConfig.class, QuerydslConfig.class})
class CartRepositoryTest {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("findByMemberId - 항목이 cascade 저장되고 회원 ID로 조회된다")
    void findByMemberId_withItems() {
        // given: (productId, optionId, quantity) — 식별 단위는 옵션
        Cart cart = Cart.create(100L);
        cart.addItem(1L, 10L, 2);
        cart.addItem(2L, 20L, 1);
        cartRepository.save(cart);
        em.flush();
        em.clear();

        // when
        Cart found = cartRepository.findByMemberId(100L).orElseThrow();

        // then
        assertThat(found.getMemberId()).isEqualTo(100L);
        assertThat(found.getCartItems()).hasSize(2);
    }

    @Test
    @DisplayName("findByMemberId - 없는 회원이면 빈 Optional")
    void findByMemberId_notFound() {
        assertThat(cartRepository.findByMemberId(999L)).isEmpty();
    }
}