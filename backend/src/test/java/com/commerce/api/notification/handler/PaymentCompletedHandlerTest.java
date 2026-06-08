package com.commerce.api.notification.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.commerce.api.global.outbox.OutboxEvent;
import com.commerce.api.notification.entity.NotificationLog;
import com.commerce.api.notification.repository.NotificationRepository;
import com.commerce.api.payment.event.PaymentCompletedPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * PaymentCompletedHandler 단위 테스트 — 알림 기록 + 멱등 소비(중복 디스패치 스킵).
 */
@ExtendWith(MockitoExtension.class)
class PaymentCompletedHandlerTest {

    @Mock
    private NotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PaymentCompletedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PaymentCompletedHandler(notificationRepository, objectMapper);
    }

    private OutboxEvent event(long id) throws Exception {
        String payload = objectMapper.writeValueAsString(new PaymentCompletedPayload(10L, 5L, 30000L));
        OutboxEvent e = OutboxEvent.pending("PAYMENT_COMPLETED", "PAYMENT", "5", payload);
        ReflectionTestUtils.setField(e, "id", id);
        return e;
    }

    @Test
    @DisplayName("결제완료 이벤트 → 알림 로그 생성(주문·금액 반영)")
    void handle_createsNotification() throws Exception {
        given(notificationRepository.existsByEventId(1L)).willReturn(false);

        handler.handle(event(1L));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationRepository).save(captor.capture());
        NotificationLog saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo("PAYMENT_COMPLETED");
        assertThat(saved.getMessage()).contains("10").contains("30000");
    }

    @Test
    @DisplayName("멱등 - 이미 처리한 이벤트면 스킵(중복 발송 안 함)")
    void handle_idempotentSkip() throws Exception {
        given(notificationRepository.existsByEventId(1L)).willReturn(true);

        handler.handle(event(1L));

        verify(notificationRepository, never()).save(any());
    }
}
