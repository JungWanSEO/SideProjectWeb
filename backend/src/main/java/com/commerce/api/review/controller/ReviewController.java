package com.commerce.api.review.controller;

import com.commerce.api.global.common.ApiResponse;
import com.commerce.api.global.common.PageResponse;
import com.commerce.api.global.security.SecurityUtil;
import com.commerce.api.review.dto.ReviewCreateRequest;
import com.commerce.api.review.dto.ReviewResponse;
import com.commerce.api.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 리뷰 API.
 * - POST   /api/products/{productId}/reviews  리뷰 작성 (인증 — 구매자만, 서비스에서 검증)
 * - GET    /api/products/{productId}/reviews  상품 리뷰 목록 (공개, 페이지)
 * - DELETE /api/reviews/{reviewId}            리뷰 삭제 (본인 또는 ADMIN)
 *
 * <p>경로 인가: GET은 SecurityConfig의 {@code GET /api/products/**} permitAll로 공개,
 * POST/DELETE는 매칭되는 공개·ADMIN 규칙이 없어 {@code anyRequest().authenticated()}로 인증 필요.
 */
@Tag(name = "리뷰(Review)", description = "상품 리뷰 작성 / 조회 / 삭제 API")
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성",
            description = "상품에 평점(1~5)·내용·사진(선택)으로 리뷰를 단다. 로그인 필요하며 "
                    + "해당 상품을 구매(PAID)한 사용자만 가능(아니면 403). 같은 상품에 이미 썼으면 409.")
    @PostMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewCreateRequest request) {
        ReviewResponse response = reviewService.create(
                SecurityUtil.getCurrentMemberId(), productId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("리뷰가 등록되었습니다.", response));
    }

    @Operation(summary = "상품 리뷰 목록 조회",
            description = "특정 상품의 리뷰를 페이지로 조회한다(공개). 기본 정렬은 최신순(createdAt desc), 기본 크기 10.")
    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> list(
            @PathVariable Long productId,
            @ParameterObject
            @PageableDefault(size = 10, sort = "createdAt", direction = Direction.DESC)
            Pageable pageable) {
        PageResponse<ReviewResponse> response = reviewService.getReviews(productId, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "리뷰 삭제",
            description = "리뷰를 삭제한다. 본인 또는 ADMIN만 가능(아니면 403). 없으면 404. 삭제 시 상품 평점 집계도 감소.")
    @DeleteMapping("/api/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long reviewId) {
        reviewService.delete(reviewId, SecurityUtil.getCurrentMemberId(), SecurityUtil.isAdmin());
        return ResponseEntity.ok(ApiResponse.<Void>success("리뷰가 삭제되었습니다.", null));
    }
}
