package com.commerce.api.order.controller;

import com.commerce.api.global.common.ApiResponse;
import com.commerce.api.global.common.PageResponse;
import com.commerce.api.global.security.SecurityUtil;
import com.commerce.api.order.dto.OrderCreateRequest;
import com.commerce.api.order.dto.OrderResponse;
import com.commerce.api.order.dto.OrderSummaryResponse;
import com.commerce.api.order.service.OrderService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 주문 API.
 * - POST /api/orders             주문 생성 (명시적 항목)
 * - POST /api/orders/checkout    장바구니 체크아웃 (장바구니 → 주문 + 비우기)
 * - GET  /api/orders             내 주문 목록 (페이지)
 * - GET  /api/orders/{id}        단건 조회
 * - POST /api/orders/{id}/cancel 주문 취소
 */
@Tag(name = "주문(Order)", description = "주문 생성 / 조회 / 취소 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "주문 생성", description = "상품 ID·수량 목록으로 주문한다. 주문자는 로그인 사용자. 주문은 결제 대기(PENDING)로 생성되며 주문 시점 가격을 스냅샷한다. 재고 차감은 결제 승인 시점에 일어난다.")
    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> create(
            @Valid @RequestBody OrderCreateRequest request) {
        OrderResponse response = orderService.create(SecurityUtil.getCurrentMemberId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문이 접수되었습니다. (결제 대기)", response));
    }

    @Operation(summary = "장바구니 체크아웃",
            description = "로그인 사용자의 장바구니를 주문으로 만들고 장바구니를 비운다(한 트랜잭션). "
                    + "주문 항목은 서버의 장바구니에서 가져온다(클라이언트가 항목을 보내지 않음). 빈 장바구니면 400. "
                    + "주문은 결제 대기(PENDING)로 생성된다(재고 차감은 결제 승인 시).")
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout() {
        OrderResponse response = orderService.checkout(SecurityUtil.getCurrentMemberId());
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("주문이 접수되었습니다. (결제 대기)", response));
    }

    @Operation(summary = "내 주문 목록 조회 (요약)",
            description = "로그인 사용자 본인의 주문을 페이지로 조회한다. 목록은 요약(대표상품명·항목수 등)만 — "
                    + "전체 항목은 단건 조회로. 기본 정렬은 최신순(createdAt desc), 기본 페이지 크기는 20. "
                    + "page/size/sort 파라미터로 변경 가능 (예: ?page=0&size=10&sort=createdAt,desc).")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderSummaryResponse>>> getMyOrders(
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Direction.DESC)
            Pageable pageable) {
        PageResponse<OrderSummaryResponse> response =
                orderService.getMyOrders(SecurityUtil.getCurrentMemberId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "주문 단건 조회",
            description = "주문 ID로 주문 정보를 조회한다. 본인 주문 또는 ADMIN만 가능(아니면 403). 없으면 404.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(@PathVariable Long id) {
        OrderResponse response = orderService.getOrder(
                id, SecurityUtil.getCurrentMemberId(), SecurityUtil.isAdmin());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "주문 취소",
            description = "주문을 취소하고 차감했던 재고를 복원한다. 본인 주문 또는 ADMIN만 가능(아니면 403). 이미 취소된 주문이면 409.")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id) {
        OrderResponse response = orderService.cancel(
                id, SecurityUtil.getCurrentMemberId(), SecurityUtil.isAdmin());
        return ResponseEntity.ok(ApiResponse.success("주문이 취소되었습니다.", response));
    }
}