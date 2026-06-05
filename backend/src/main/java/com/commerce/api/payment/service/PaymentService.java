package com.commerce.api.payment.service;

import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.order.dto.OrderResponse;
import com.commerce.api.order.entity.OrderStatus;
import com.commerce.api.order.service.OrderService;
import com.commerce.api.payment.dto.PaymentRequest;
import com.commerce.api.payment.dto.PaymentResponse;
import com.commerce.api.payment.entity.Payment;
import com.commerce.api.payment.gateway.PaymentApproval;
import com.commerce.api.payment.gateway.PaymentGateway;
import com.commerce.api.payment.gateway.PaymentGateway.PaymentApprovalCommand;
import com.commerce.api.payment.repository.PaymentRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 결제 오케스트레이터.
 *
 * <p>흐름: 멱등성 확인 → 주문 검증(본인·PENDING) → PG 승인(모의) → 승인 시 재고 차감+주문 PAID → 결제 PAID.
 *
 * <p><b>의도적으로 @Transactional을 두지 않는다.</b> 재고 차감은 {@link OrderService#pay}(자체 @Transactional
 * + @Retryable)에 위임하는데, 이 메서드를 트랜잭션으로 감싸면 내부 호출이 같은 트랜잭션에 합류해
 * "새 트랜잭션으로 재시도"가 깨진다(낙관적 락 재시도는 트랜잭션 바깥에서 새 트랜잭션을 열어야 함).
 * 결제 저장은 {@code paymentRepository.save}가 각자 트랜잭션으로 처리한다.
 *
 * <p>※ 주문 PAID 커밋과 결제 PAID 저장 사이의 원자성(크로스 애그리거트)은 단순화했다 —
 * 운영에서는 이벤트/아웃박스로 보강한다(architecture.md §13.5).
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderService orderService;
    private final PaymentGateway paymentGateway;

    public PaymentResponse pay(Long memberId, PaymentRequest request) {
        // 1) 멱등성: 같은 키로 이미 처리된 결제가 있으면 재실행 없이 그 결과를 반환
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) {
            return PaymentResponse.from(existing.get());
        }

        // 2) 주문 검증: 존재 + 본인(아니면 OrderService가 404/403) + 결제 가능(PENDING)
        OrderResponse order = orderService.getOrder(request.orderId(), memberId, false);
        if (order.status() != OrderStatus.PENDING) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "결제할 수 없는 주문 상태입니다. (현재: " + order.status() + ")");
        }

        long amount = order.totalPrice();
        String method = (request.method() == null || request.method().isBlank()) ? "MOCK_CARD" : request.method();
        Payment payment = Payment.ready(order.id(), amount, method, request.idempotencyKey());

        // 3) PG 승인 요청 (모의)
        PaymentApproval approval = paymentGateway.approve(
                new PaymentApprovalCommand(order.id(), amount, request.idempotencyKey()));
        if (!approval.approved()) {
            payment.markFailed();
            paymentRepository.save(payment);
            throw new BusinessException(HttpStatus.PAYMENT_REQUIRED,
                    "결제가 거절되었습니다. (" + approval.failureReason() + ")");
        }

        // 4) 승인 성공 → 재고 차감 + 주문 PAID (낙관적 락 재시도 포함). 재고 부족 등 실패면 결제 FAILED로 기록 후 전파.
        try {
            orderService.pay(order.id());
        } catch (RuntimeException e) {
            payment.markFailed();
            paymentRepository.save(payment);
            throw e;   // 주문은 PENDING으로 남는다(재고 보충 후 재결제 가능)
        }
        payment.markPaid(approval.pgTransactionId());
        paymentRepository.save(payment);
        return PaymentResponse.from(payment);
    }
}
