package com.commerce.api.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.commerce.api.cart.entity.Cart;
import com.commerce.api.cart.repository.CartRepository;
import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.order.dto.OrderCreateRequest;
import com.commerce.api.order.dto.OrderCreateRequest.OrderItemRequest;
import com.commerce.api.order.dto.OrderResponse;
import com.commerce.api.order.entity.Order;
import com.commerce.api.order.repository.OrderRepository;
import com.commerce.api.product.entity.Product;
import com.commerce.api.product.entity.ProductOption;
import com.commerce.api.product.entity.ProductStatus;
import com.commerce.api.product.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OrderProcessor 단위 테스트 — 주문 처리(옵션 재고 차감·스냅샷·총액) 로직.
 */
@ExtendWith(MockitoExtension.class)
class OrderProcessorTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderProcessor orderProcessor;

    /** id가 채워진 옵션("M") 1개를 가진 상품 생성. */
    private Product productWithOption(Long productId, Long optionId, String name, long price, int stock) {
        Product product = Product.builder()
                .name(name).price(price).description("desc").status(ProductStatus.ON_SALE).build();
        ReflectionTestUtils.setField(product, "id", productId);
        ProductOption option = ProductOption.create("M", stock);
        ReflectionTestUtils.setField(option, "id", optionId);
        product.addOption(option);
        return product;
    }

    @Test
    @DisplayName("주문 처리 성공 - 옵션 재고 차감, 가격·사이즈 스냅샷, 총액 계산")
    void place_success() {
        Product product = productWithOption(1L, 10L, "반팔티셔츠", 10000L, 10);
        given(productRepository.findByOptionId(10L)).willReturn(Optional.of(product));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderProcessor.place(100L,
                new OrderCreateRequest(List.of(new OrderItemRequest(10L, 3))));

        assertThat(response.totalPrice()).isEqualTo(30000L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("반팔티셔츠");
        assertThat(response.items().get(0).size()).isEqualTo("M");
        assertThat(response.items().get(0).subtotal()).isEqualTo(30000L);
        assertThat(product.getOptions().get(0).getStock()).isEqualTo(7);   // 10 - 3
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 처리 실패 - 옵션 재고 부족이면 예외, 저장 안 됨")
    void place_insufficientStock() {
        Product product = productWithOption(1L, 10L, "반팔티셔츠", 10000L, 2);
        given(productRepository.findByOptionId(10L)).willReturn(Optional.of(product));

        assertThatThrownBy(() -> orderProcessor.place(100L,
                new OrderCreateRequest(List.of(new OrderItemRequest(10L, 5)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("재고가 부족합니다");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("주문 처리 실패 - 존재하지 않는 옵션")
    void place_optionNotFound() {
        given(productRepository.findByOptionId(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderProcessor.place(100L,
                new OrderCreateRequest(List.of(new OrderItemRequest(99L, 1)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("옵션을 찾을 수 없습니다");
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    @DisplayName("체크아웃 - 장바구니를 주문으로 만들고 장바구니를 비운다")
    void checkout_createsOrderAndClearsCart() {
        Product product = productWithOption(1L, 10L, "반팔티셔츠", 10000L, 10);
        Cart cart = Cart.create(100L);
        cart.addItem(1L, 10L, 2);   // (productId, optionId, quantity)
        given(cartRepository.findByMemberId(100L)).willReturn(Optional.of(cart));
        given(productRepository.findByOptionId(10L)).willReturn(Optional.of(product));
        given(orderRepository.save(any(Order.class))).willAnswer(inv -> inv.getArgument(0));

        OrderResponse response = orderProcessor.checkout(100L);

        assertThat(response.totalPrice()).isEqualTo(20000L);              // 10000 x 2
        assertThat(response.items()).hasSize(1);
        assertThat(product.getOptions().get(0).getStock()).isEqualTo(8);  // 10 - 2
        assertThat(cart.getCartItems()).isEmpty();                        // 장바구니 비워짐
    }

    @Test
    @DisplayName("체크아웃 실패 - 빈 장바구니면 400, 저장 안 됨")
    void checkout_emptyCart() {
        Cart cart = Cart.create(100L);   // 항목 없음
        given(cartRepository.findByMemberId(100L)).willReturn(Optional.of(cart));

        assertThatThrownBy(() -> orderProcessor.checkout(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("장바구니가 비어 있습니다");
        verify(orderRepository, never()).save(any(Order.class));
    }
}
