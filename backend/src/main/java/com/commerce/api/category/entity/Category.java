package com.commerce.api.category.entity;

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
 * 상품 카테고리 (예: 상의, 하의, 신발). 지금은 평면 구조(계층 없음 — 후속 스트레치).
 *
 * <p>별도 애그리거트다. 다른 도메인(Product 등)은 객체 연관이 아니라 <b>categoryId(Long)</b>로
 * 참조한다(architecture.md §11 "애그리거트 간 ID 참조 · 객체 연관 금지").
 */
@Getter
@Entity
@Table(name = "category")
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA가 쓰는 기본 생성자(외부 직접 생성은 막음)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private Category(String name) {
        this.name = name;
    }

    /** 정적 팩토리 — 의미 있는 생성 지점. */
    public static Category create(String name) {
        return new Category(name);
    }
}
