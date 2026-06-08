package com.commerce.api.payment.service;

import com.commerce.api.global.outbox.OutboxService;
import com.commerce.api.payment.entity.Payment;
import com.commerce.api.payment.event.PaymentCompletedPayload;
import com.commerce.api.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 완료를 "원자적으로" 기록한다 — 결제 저장 + 아웃박스 이벤트를 <b>한 트랜잭션</b>으로.
 *
 * <p>{@link PaymentService}와 별도 빈인 이유: PaymentService.pay는 (재시도 보존을 위해) 트랜잭션이 없다.
 * 그 안에서 @Transactional 메서드를 self-invocation하면 프록시가 안 걸려 트랜잭션이 무효가 된다 →
 * "다른 빈"으로 빼야 트랜잭션 경계가 실제로 생긴다. (Spring AOP 함정 — docs/event-outbox-design.md §4.1.)
 */
@Component
@RequiredArgsConstructor
public class PaymentCompletionRecorder {

    private final PaymentRepository paymentRepository;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    /**
     * 결제(이미 markPaid된 상태)를 저장하고, 같은 트랜잭션에서 PAYMENT_COMPLETED 이벤트를 아웃박스에 기록한다.
     * 둘이 한 커밋이라 "결제는 됐는데 이벤트 유실" 또는 그 반대가 구조적으로 불가능하다.
     */
    @Transactional
    public void saveWithEvent(Payment payment) {
        paymentRepository.save(payment);   // PAID 상태 영속(IDENTITY로 id 생성)
        outboxService.append(
                "PAYMENT_COMPLETED",
                "PAYMENT",
                String.valueOf(payment.getId()),
                toJson(new PaymentCompletedPayload(payment.getOrderId(), payment.getId(), payment.getAmount())));
    }

    private String toJson(PaymentCompletedPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이벤트 페이로드 직렬화 실패", e);
        }
    }
}
