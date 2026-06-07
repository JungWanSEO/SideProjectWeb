package com.commerce.api.payment.controller;

import com.commerce.api.global.common.ApiResponse;
import com.commerce.api.global.security.SecurityUtil;
import com.commerce.api.payment.dto.PaymentRequest;
import com.commerce.api.payment.dto.PaymentResponse;
import com.commerce.api.payment.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 API.
 * - POST /api/payments   PENDING 주문을 결제(모의 PG) → 승인 시 재고 차감 + 주문 PAID.
 */
@Tag(name = "결제(Payment)", description = "결제 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제",
            description = "PENDING 주문을 결제한다(모의 PG). 승인 시 옵션 재고를 차감하고 주문을 PAID로 만든다. "
                    + "idempotencyKey로 중복 결제를 막는다(같은 키 재요청은 첫 결과 반환). "
                    + "본인 주문만 결제 가능(아니면 403/404), 결제 불가 상태면 409, PG 거절 시 402.")
    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> pay(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = paymentService.pay(SecurityUtil.getCurrentMemberId(), request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("결제가 완료되었습니다.", response));
    }
}
