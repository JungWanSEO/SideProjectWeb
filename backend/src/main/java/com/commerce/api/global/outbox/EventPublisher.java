package com.commerce.api.global.outbox;

/**
 * 이벤트 발행 포트 — 아웃박스 폴러가 이벤트를 "어디로 보낼지"의 추상화.
 *
 * <p>지금은 {@link InProcessEventPublisher}(같은 프로세스 핸들러로 디스패치)지만, 운영에선 같은 포트에
 * RabbitMQ/Kafka 어댑터를 구현해 교체하면 된다 — 폴러·결제 코드는 포트에만 의존하므로 코드 변경이 없다
 * (PaymentGateway와 같은 포트-어댑터·DIP).
 */
public interface EventPublisher {

    /** 이벤트를 발행한다. 실패 시 예외를 던진다(폴러가 재시도). */
    void publish(OutboxEvent event);
}
