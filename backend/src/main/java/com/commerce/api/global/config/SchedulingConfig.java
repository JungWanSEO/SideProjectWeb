package com.commerce.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 스케줄링 활성화 — 아웃박스 폴러({@code @Scheduled})가 동작하도록.
 * (폴러 자체는 {@code outbox.relay.enabled}로 켜고 끈다 — 테스트에선 off.)
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
