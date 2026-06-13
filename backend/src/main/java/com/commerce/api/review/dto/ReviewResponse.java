package com.commerce.api.review.dto;

import com.commerce.api.review.entity.Review;
import java.time.LocalDateTime;

/**
 * 리뷰 응답 DTO. 작성자 식별은 memberId + 표시용 writerName(닉네임)으로 노출(이메일 등 민감정보 미노출).
 */
public record ReviewResponse(
        Long id,
        Long memberId,
        String writerName,   // 작성자 닉네임(enrich) — 없으면(탈퇴 등) null
        Long productId,
        int rating,
        String content,
        String imageUrl,     // 사진리뷰(없으면 null)
        LocalDateTime createdAt
) {
    public static ReviewResponse of(Review review, String writerName) {
        return new ReviewResponse(
                review.getId(),
                review.getMemberId(),
                writerName,
                review.getProductId(),
                review.getRating(),
                review.getContent(),
                review.getImageUrl(),
                review.getCreatedAt()
        );
    }
}
