package com.commerce.api.category.dto;

import com.commerce.api.category.entity.Category;

/**
 * 카테고리 응답. (엔티티를 직접 노출하지 않고 DTO로 변환)
 */
public record CategoryResponse(Long id, String name) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
