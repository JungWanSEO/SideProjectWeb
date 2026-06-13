package com.commerce.api.review.entity;

import com.commerce.api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 리뷰 (review 테이블).
 *
 * - 작성자(회원)·대상 상품은 다른 애그리거트 → ID 참조(memberId/productId).
 * - <b>1인 1상품 1리뷰</b>: (member_id, product_id) UNIQUE.
 * - rating은 1~5 정수(검증은 요청 DTO에서). imageUrl은 사진리뷰(선택).
 * - 평점 집계는 여기서 하지 않는다 — Product의 비정규화 카운터(ratingCount/ratingSum)를 작성/삭제 시 갱신.
 */
@Getter
@Entity
@Table(name = "review", uniqueConstraints = @UniqueConstraint(
        name = "uk_review_member_product", columnNames = {"member_id", "product_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long memberId;    // 작성자(회원) → ID 참조

    @Column(nullable = false)
    private Long productId;   // 대상 상품 → ID 참조

    @Column(nullable = false)
    private int rating;       // 1~5

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(length = 500)
    private String imageUrl;  // 사진리뷰(선택) — 상품 imageUrl과 동일한 패턴

    @Builder
    private Review(Long memberId, Long productId, int rating, String content, String imageUrl) {
        this.memberId = memberId;
        this.productId = productId;
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
    }

    /** 작성자가 리뷰를 수정. 평점 변화량은 서비스가 상품 평점 합계에 반영한다(memberId/productId는 불변). */
    public void update(int rating, String content, String imageUrl) {
        this.rating = rating;
        this.content = content;
        this.imageUrl = imageUrl;
    }
}
