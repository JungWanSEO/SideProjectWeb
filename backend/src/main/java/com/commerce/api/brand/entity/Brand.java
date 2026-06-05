package com.commerce.api.brand.entity;

import com.commerce.api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드 (예: Nike, Adidas).
 *
 * <p>별도 애그리거트다. 다른 도메인(Product 등)은 객체 연관이 아니라 <b>brandId(Long)</b>로
 * 참조한다(architecture.md §11 "애그리거트 간 ID 참조 · 객체 연관 금지").
 */
@Getter
@Entity
@Table(name = "brand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Brand extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private Brand(String name) {
        this.name = name;
    }

    public static Brand create(String name) {
        return new Brand(name);
    }
}
