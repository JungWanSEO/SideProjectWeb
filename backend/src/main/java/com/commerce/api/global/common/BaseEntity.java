package com.commerce.api.global.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 모든 엔티티가 상속하는 공통 베이스.
 * 생성/수정 일시를 JPA Auditing이 자동으로 채워준다.
 *
 * - @MappedSuperclass: 이 클래스 자체는 테이블이 아니고, 자식 엔티티의 컬럼으로 내려간다.
 * - @EntityListeners(AuditingEntityListener): 저장/수정 시점에 일시를 자동 주입.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
