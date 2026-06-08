package com.commerce.api.global.outbox;

/**
 * 아웃박스 이벤트 소비자(핸들러).
 *
 * <p>{@link #eventType()}이 일치하는 이벤트를 {@link #handle}이 처리한다. 여러 핸들러가 같은 타입을
 * 구독하면 팬아웃된다. 발행은 at-least-once라 같은 이벤트가 두 번 올 수 있으므로 <b>핸들러는 멱등</b>해야 한다.
 */
public interface OutboxEventHandler {

    /** 이 핸들러가 구독하는 이벤트 타입 (예: "PAYMENT_COMPLETED"). */
    String eventType();

    /** 이벤트 처리. 실패하면 예외를 던진다 → 폴러가 재시도한다. */
    void handle(OutboxEvent event);
}
