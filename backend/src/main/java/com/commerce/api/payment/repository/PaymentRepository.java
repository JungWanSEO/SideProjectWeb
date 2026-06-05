package com.commerce.api.payment.repository;

import com.commerce.api.payment.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /** 멱등키로 기존 결제 조회 — 같은 키의 중복 요청을 감지해 재실행을 막는다. */
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
