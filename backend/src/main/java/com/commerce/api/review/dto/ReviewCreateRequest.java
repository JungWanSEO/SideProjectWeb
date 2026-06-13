package com.commerce.api.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 리뷰 작성 요청 DTO. (대상 상품은 경로변수 productId, 작성자는 로그인 사용자 → 본문엔 평점·내용·사진만)
 */
@Schema(description = "리뷰 작성 요청")
public record ReviewCreateRequest(

        @Schema(description = "평점(1~5)", example = "5")
        @NotNull(message = "평점은 필수입니다.")
        @Min(value = 1, message = "평점은 1 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 5 이하여야 합니다.")
        Integer rating,

        @Schema(description = "리뷰 내용(1000자 이하)", example = "핏이 좋고 색감이 사진과 똑같아요.")
        @NotBlank(message = "리뷰 내용은 필수입니다.")
        @Size(max = 1000, message = "리뷰 내용은 1000자 이하여야 합니다.")
        String content,

        @Schema(description = "사진 URL(선택, 500자 이하)", example = "/products/tee.svg")
        @Size(max = 500, message = "이미지 URL은 500자 이하여야 합니다.")
        String imageUrl
) {
}
