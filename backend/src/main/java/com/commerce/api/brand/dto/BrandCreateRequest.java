package com.commerce.api.brand.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 브랜드 등록 요청.
 */
public record BrandCreateRequest(
        @NotBlank(message = "브랜드명은 필수입니다.") String name
) {
}
