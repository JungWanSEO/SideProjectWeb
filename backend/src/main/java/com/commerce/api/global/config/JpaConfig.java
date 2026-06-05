package com.commerce.api.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * JPA 관련 설정.
 * @EnableJpaAuditing: BaseEntity의 생성/수정일시 자동 기록(@CreatedDate, @LastModifiedDate)을 켠다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
