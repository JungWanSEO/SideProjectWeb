package com.commerce.api.global.outbox;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * in-process 이벤트 발행 어댑터 — 외부 브로커 없이 같은 프로세스의 핸들러로 디스패치한다.
 *
 * <p>이벤트 타입별로 핸들러를 모아두고, 발행 시 해당 타입의 핸들러들을 순서대로 호출한다(팬아웃 가능).
 * 핸들러가 던지면 예외가 폴러까지 전파되어 재시도된다.
 */
@Component
public class InProcessEventPublisher implements EventPublisher {

    private final Map<String, List<OutboxEventHandler>> handlersByType;

    public InProcessEventPublisher(List<OutboxEventHandler> handlers) {
        // 스프링이 모든 OutboxEventHandler 빈을 주입 → eventType으로 그룹핑
        this.handlersByType = handlers.stream().collect(Collectors.groupingBy(OutboxEventHandler::eventType));
    }

    @Override
    public void publish(OutboxEvent event) {
        for (OutboxEventHandler handler : handlersByType.getOrDefault(event.getEventType(), List.of())) {
            handler.handle(event);
        }
        // 구독자가 없으면 no-op → 폴러가 PUBLISHED 처리(소비자 없는 이벤트는 그냥 소모).
    }
}
