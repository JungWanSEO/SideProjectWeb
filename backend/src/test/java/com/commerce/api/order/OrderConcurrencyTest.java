package com.commerce.api.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.commerce.api.order.dto.OrderCreateRequest;
import com.commerce.api.order.dto.OrderCreateRequest.OrderItemRequest;
import com.commerce.api.order.dto.OrderResponse;
import com.commerce.api.order.service.OrderService;
import com.commerce.api.product.entity.Product;
import com.commerce.api.product.entity.ProductOption;
import com.commerce.api.product.entity.ProductStatus;
import com.commerce.api.product.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 재고 동시성 통합 테스트.
 * 재고 차감이 "결제 승인(pay)" 시점으로 이동했으므로, 여러 스레드가 같은 옵션을 동시에 결제해도
 * 옵션 @Version 낙관적 락 덕분에 초과 판매가 없음을 검증한다.
 */
@SpringBootTest
class OrderConcurrencyTest {

    @Autowired
    private OrderService orderService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private PlatformTransactionManager txManager;   // 최종 재고 읽기를 트랜잭션 안에서 하기 위함

    @Test
    @DisplayName("동시 결제 - 초과 판매가 발생하지 않는다 (옵션 재고 기준, lost update 없음)")
    void concurrentPayments_noOversell() throws InterruptedException {
        // given: 재고 10개짜리 옵션 1개를 가진 상품
        int initialStock = 10;
        int threadCount = 20;
        Product product = Product.builder()
                .name("한정수량상품").price(10000L).description("desc").status(ProductStatus.ON_SALE).build();
        product.addOption(ProductOption.create("M", initialStock));
        Product saved = productRepository.save(product);
        Long optionId = saved.getOptions().get(0).getId();

        // 20개의 PENDING 주문을 먼저 생성한다 (주문 생성은 재고를 차감하지 않으므로 모두 성공).
        List<Long> orderIds = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            OrderResponse order = orderService.create(1L,
                    new OrderCreateRequest(List.of(new OrderItemRequest(optionId, 1))));
            orderIds.add(order.id());
        }

        ExecutorService executor = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();

        // when: 20개 스레드가 각자의 주문을 동시에 결제한다 (재고 차감은 여기서 — 경합 발생).
        for (Long orderId : orderIds) {
            executor.submit(() -> {
                try {
                    orderService.pay(orderId);
                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // then: 초과 판매 없음 (불변식) — 옵션 재고 기준
        // 옵션 컬렉션은 지연 로딩이라 트랜잭션 안에서 읽어 초기화한다(테스트 메서드 자체는 비트랜잭션).
        int remaining = new TransactionTemplate(txManager).execute(s ->
                productRepository.findById(saved.getId()).orElseThrow().getOptions().get(0).getStock());
        assertThat(success.get()).isLessThanOrEqualTo(initialStock);            // ① 성공 ≤ 재고
        assertThat(remaining).isGreaterThanOrEqualTo(0);                        // ② 재고 음수 없음
        assertThat(success.get()).isEqualTo(initialStock - remaining);         // ③ 판매량 = 차감량
        assertThat(success.get() + fail.get()).isEqualTo(threadCount);         // 모든 결제 처리됨
    }
}
