package com.commerce.api.global.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OutboxProcessor 단위 테스트 — 발행 성공/스킵/실패 전파 + 재시도 누적·데드레터·지수 백오프.
 */
@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

    private static final long BASE_MS = 2000;
    private static final long MAX_MS = 60000;

    @Mock
    private OutboxRepository outboxRepository;
    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private OutboxProcessor outboxProcessor;

    @BeforeEach
    void setBackoff() {
        // @Value 필드는 단위 테스트(@InjectMocks)에서 주입되지 않으므로 직접 설정한다.
        ReflectionTestUtils.setField(outboxProcessor, "backoffBaseMs", BASE_MS);
        ReflectionTestUtils.setField(outboxProcessor, "backoffMaxMs", MAX_MS);
    }

    private OutboxEvent pendingEvent() {
        OutboxEvent e = OutboxEvent.pending("PAYMENT_COMPLETED", "PAYMENT", "5", "{}");
        ReflectionTestUtils.setField(e, "id", 1L); // IDENTITY id는 영속 전 null → 테스트에서 주입
        return e;
    }

    @Test
    @DisplayName("발행 성공 - 핸들러 호출 후 PUBLISHED")
    void publish_success() {
        OutboxEvent event = pendingEvent();
        given(outboxRepository.findPendingForUpdate(1L)).willReturn(Optional.of(event));

        outboxProcessor.publish(1L);

        verify(eventPublisher).publish(event);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("발행 스킵 - 잠금 클레임이 비면 발행하지 않음(이미 처리됨/다른 폴러가 잠금)")
    void publish_skipsWhenClaimEmpty() {
        // findPendingForUpdate는 SQL에서 status='PENDING' + SKIP LOCKED를 거르므로,
        // 빈 결과 = 비PENDING이거나 다른 폴러가 처리 중인 행.
        given(outboxRepository.findPendingForUpdate(1L)).willReturn(Optional.empty());

        outboxProcessor.publish(1L);

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("발행 실패 - 핸들러 예외는 전파되고 PUBLISHED로 안 바뀜(트랜잭션 롤백 대상)")
    void publish_handlerThrows_propagates() {
        OutboxEvent event = pendingEvent();
        given(outboxRepository.findPendingForUpdate(1L)).willReturn(Optional.of(event));
        willThrow(new RuntimeException("핸들러 실패")).given(eventPublisher).publish(event);

        assertThatThrownBy(() -> outboxProcessor.publish(1L)).isInstanceOf(RuntimeException.class);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING); // 발행 안 됨 → 재시도 대상
    }

    @Test
    @DisplayName("실패 기록 - 재시도 횟수 누적 + 백오프 시각 설정(아직 PENDING)")
    void recordFailure_incrementsRetry() {
        OutboxEvent event = pendingEvent();
        given(outboxRepository.findById(1L)).willReturn(Optional.of(event));
        LocalDateTime before = LocalDateTime.now();

        outboxProcessor.recordFailure(1L, "PG 점검중");

        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getLastError()).isEqualTo("PG 점검중");
        // 1회 실패 → 약 base(2s) 뒤로 미뤄짐
        assertThat(event.getNextAttemptAt())
            .isAfter(before.plusSeconds(1))
            .isBefore(before.plusSeconds(4));
    }

    @Test
    @DisplayName("실패 기록 - 재시도가 거듭되면 백오프 간격이 지수적으로 증가(2s→4s→8s)")
    void recordFailure_backoffGrowsExponentially() {
        OutboxEvent event = pendingEvent();
        given(outboxRepository.findById(1L)).willReturn(Optional.of(event));
        LocalDateTime t0 = LocalDateTime.now();

        outboxProcessor.recordFailure(1L, "1차"); // retryCount 0→1 : ~2s
        assertThat(event.getNextAttemptAt()).isBetween(t0.plusSeconds(1), t0.plusSeconds(3));

        outboxProcessor.recordFailure(1L, "2차"); // retryCount 1→2 : ~4s
        assertThat(event.getNextAttemptAt()).isBetween(t0.plusSeconds(3), t0.plusSeconds(6));

        outboxProcessor.recordFailure(1L, "3차"); // retryCount 2→3 : ~8s
        assertThat(event.getNextAttemptAt()).isBetween(t0.plusSeconds(7), t0.plusSeconds(11));
    }

    @Test
    @DisplayName("실패 기록 - 최대 재시도 초과 시 FAILED(데드레터)")
    void recordFailure_maxRetries_toFailed() {
        OutboxEvent event = pendingEvent();
        given(outboxRepository.findById(1L)).willReturn(Optional.of(event));

        for (int i = 0; i < OutboxProcessor.MAX_RETRIES; i++) {
            outboxProcessor.recordFailure(1L, "err");
        }

        assertThat(event.getRetryCount()).isEqualTo(OutboxProcessor.MAX_RETRIES);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.FAILED);
    }
}
