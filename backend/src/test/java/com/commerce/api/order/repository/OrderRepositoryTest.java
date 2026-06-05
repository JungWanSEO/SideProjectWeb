package com.commerce.api.order.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.commerce.api.global.config.JpaConfig;
import com.commerce.api.global.config.QuerydslConfig;
import com.commerce.api.order.entity.Order;
import com.commerce.api.order.entity.OrderItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * OrderRepository 슬라이스 테스트 (@DataJpaTest).
 * flush/clear로 영속성 컨텍스트를 비운 뒤 재조회하여, 애그리거트(주문+항목)가
 * cascade로 DB에 실제 저장되는지 검증한다.
 */
@DataJpaTest
// QuerydslConfig: @DataJpaTest가 모든 리포지토리를 로드하므로 ProductRepository(QueryDSL)의
// JPAQueryFactory 빈이 슬라이스에도 필요하다.
@Import({JpaConfig.class, QuerydslConfig.class})
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("주문 저장 시 항목이 cascade로 함께 저장되고, 총액·생성일시가 채워진다")
    void save_cascadesItemsAndTotal() {
        // given
        Order order = Order.create(100L);
        order.addItem(OrderItem.builder()
                .productId(1L).optionId(11L).productName("반팔티셔츠").size("M")
                .orderPrice(10000L).quantity(3).build());
        order.addItem(OrderItem.builder()
                .productId(2L).optionId(21L).productName("청바지").size("L")
                .orderPrice(20000L).quantity(1).build());

        // when
        Long savedId = orderRepository.save(order).getId();
        em.flush();
        em.clear();   // 영속성 컨텍스트 비움 → 아래 조회는 DB에서 다시 읽어옴

        // then
        Order found = orderRepository.findById(savedId).orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getTotalPrice()).isEqualTo(50000L);   // 10000*3 + 20000*1
        assertThat(found.getOrderItems()).hasSize(2);
    }

    @Test
    @DisplayName("findById - 없는 id면 빈 Optional")
    void findById_notFound() {
        assertThat(orderRepository.findById(999L)).isEmpty();
    }

    private Order orderFor(Long memberId) {
        Order order = Order.create(memberId);
        order.addItem(OrderItem.builder()
                .productId(1L).optionId(11L).productName("반팔티셔츠").size("M")
                .orderPrice(10000L).quantity(1).build());
        return order;
    }

    @Test
    @DisplayName("findByMemberId - 해당 회원의 주문만 조회되고 다른 회원 주문은 제외된다")
    void findByMemberId_onlyOwnOrders() {
        orderRepository.save(orderFor(100L));
        orderRepository.save(orderFor(100L));
        orderRepository.save(orderFor(200L));   // 다른 회원
        em.flush();
        em.clear();

        Page<Order> page = orderRepository.findByMemberId(
                100L, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(Order::getMemberId)
                .containsOnly(100L);
    }

    @Test
    @DisplayName("findByMemberId - 페이지 크기만큼 담고 totalPages/hasNext로 다음을 알린다")
    void findByMemberId_paging() {
        for (int i = 0; i < 3; i++) {
            orderRepository.save(orderFor(100L));
        }
        em.flush();
        em.clear();

        Page<Order> first = orderRepository.findByMemberId(100L, PageRequest.of(0, 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.hasNext()).isTrue();
    }
}