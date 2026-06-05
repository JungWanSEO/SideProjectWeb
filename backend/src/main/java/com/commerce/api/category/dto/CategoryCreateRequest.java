package com.commerce.api.category.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 카테고리 등록 요청.
 */
public record CategoryCreateRequest(
        @NotBlank(message = "카테고리명은 필수입니다.") String name
) {
}
