package com.commerce.api.cart.controller;

import com.commerce.api.cart.dto.CartItemAddRequest;
import com.commerce.api.cart.dto.CartResponse;
import com.commerce.api.cart.service.CartService;
import com.commerce.api.global.common.ApiResponse;
import com.commerce.api.global.security.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 장바구니 API. 대상 회원은 인증된 로그인 사용자.
 * - POST   /api/carts/items                 담기 (옵션 단위)
 * - GET    /api/carts                        조회
 * - DELETE /api/carts/items/{optionId}      항목 제거 (옵션 단위)
 */
@Tag(name = "장바구니(Cart)", description = "담기 / 조회 / 항목 제거 API")
@RestController
@RequestMapping("/api/carts")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @Operation(summary = "장바구니 담기", description = "옵션(사이즈)을 담는다. 같은 옵션이면 수량을 더한다. 장바구니가 없으면 생성.")
    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @Valid @RequestBody CartItemAddRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "장바구니에 담았습니다.", cartService.addItem(SecurityUtil.getCurrentMemberId(), request)));
    }

    @Operation(summary = "장바구니 조회", description = "로그인 사용자의 장바구니를 현재 상품 정보(이름/가격)로 채워 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart() {
        return ResponseEntity.ok(ApiResponse.success(cartService.getCart(SecurityUtil.getCurrentMemberId())));
    }

    @Operation(summary = "장바구니 항목 제거", description = "장바구니에서 특정 옵션(사이즈) 항목을 제거한다.")
    @DeleteMapping("/items/{optionId}")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(@PathVariable Long optionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "항목을 제거했습니다.", cartService.removeItem(SecurityUtil.getCurrentMemberId(), optionId)));
    }
}
