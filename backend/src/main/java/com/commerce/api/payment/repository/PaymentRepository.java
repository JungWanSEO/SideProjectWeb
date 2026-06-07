package com.commerce.api.payment.repository;

import com.commerce.api.payment.entity.Payment;
import com.commerce.api.payment.entity.PaymentStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 멱등키로 기존 결제 조회 — 같은 키의 중복 요청을 감지해 재실행을 막는다. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    /**
     * 주문의 특정 상태 결제 조회 — 환불 시 PAID 한 건을 찾는 데 쓴다.
     * (한 주문에 결제 시도가 여러 번이어도 PAID는 최대 1건 → Optional로 안전.)
     */
    Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
