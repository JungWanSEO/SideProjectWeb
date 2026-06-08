package com.commerce.api.notification.handler;

import com.commerce.api.global.outbox.OutboxEvent;
import com.commerce.api.global.outbox.OutboxEventHandler;
import com.commerce.api.notification.entity.NotificationLog;
import com.commerce.api.notification.repository.NotificationRepository;
import com.commerce.api.payment.event.PaymentCompletedPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PAYMENT_COMPLETED 이벤트 소비자 — 결제 완료 시 "알림 발송"을 기록한다(모의).
 *
 * <p><b>멱등</b>: at-least-once 발행이라 같은 이벤트가 두 번 올 수 있다. {@code existsByEventId}로 먼저 걸러내고,
 * DB의 {@code event_id} UNIQUE가 최후 방어선이 된다(대사의 예외 큐와 같은 "중복은 정상 시나리오" 사고).
 */
@Component
@RequiredArgsConstructor
public class PaymentCompletedHandler implements OutboxEventHandler {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;

    @Override
    public String eventType() {
        return "PAYMENT_COMPLETED";
    }

    @Override
    public void handle(OutboxEvent event) {
        if (notificationRepository.existsByEventId(event.getId())) {
            return; // 이미 처리한 이벤트 → 스킵(멱등)
        }
        PaymentCompletedPayload payload = parse(event.getPayload());
        String message = "결제 완료 알림 — 주문 #" + payload.orderId() + " · " + payload.amount() + "원";
        notificationRepository.save(NotificationLog.of(event.getId(), event.getEventType(), message));
    }

    private PaymentCompletedPayload parse(String json) {
        try {
            return objectMapper.readValue(json, PaymentCompletedPayload.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("이벤트 페이로드 역직렬화 실패", e);
        }
    }
}
