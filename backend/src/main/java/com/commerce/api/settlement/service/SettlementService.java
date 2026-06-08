package com.commerce.api.settlement.service;

import com.commerce.api.global.common.PageResponse;
import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.payment.dto.PaymentResponse;
import com.commerce.api.payment.service.PaymentService;
import com.commerce.api.settlement.dto.SettlementResponse;
import com.commerce.api.settlement.dto.SettlementRunResponse;
import com.commerce.api.settlement.entity.SettlementEntry;
import com.commerce.api.settlement.repository.SettlementRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정산 서비스.
 *
 * <p>결제(payment)는 상위 도메인이고 정산(settlement)은 그것을 읽어 가공하는 하위 도메인이다 —
 * 의존 방향은 settlement → payment 한 방향(역방향이면 순환). 그래서 결제 데이터는 PaymentService를
 * 통해 DTO로만 받는다(엔티티를 가로질러 만지지 않는다 = 도메인 경계 유지, add-domain 컨벤션).
 */
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final PaymentService paymentService;

    /**
     * 정산 배치 — PAID 결제 중 아직 정산 항목이 없는 건을 모아 SettlementEntry(SCHEDULED)를 만든다.
     *
     * <p>실무의 "T+N 일배치"를 모델링한다: 결제 승인은 실시간이지만 정산은 며칠 뒤 별도 시점에 일어난다
     * (다른 시점·다른 도메인). 그래서 결제 쓰기 경로(PaymentService.pay)는 건드리지 않고, 여기서 별도로 스캔한다.
     *
     * <p>멱등성: 같은 결제를 두 번 잡지 않도록 {@code existsByPaymentId}로 거른다(+ DB의 payment_id UNIQUE).
     * 그래서 배치를 여러 번 돌려도 안전하다.
     *
     * <p>단순화: 실제 배치는 "정산 대상일 윈도우"로 필터하지만, 여기선 미정산 PAID 전체를 스캔한다.
     * 또 PAID였다가 환불(CANCELLED)된 결제는 status가 PAID가 아니므로 자연히 제외된다(정산 후 취소분의
     * 상계는 대사(P2)의 몫).
     */
    @Transactional
    public SettlementRunResponse run() {
        LocalDate settledDate = LocalDate.now().plusDays(SettlementPolicy.PAYOUT_DELAY_DAYS);

        int created = 0;
        long totalGross = 0;
        long totalFee = 0;
        long totalNet = 0;

        for (PaymentResponse payment : paymentService.getPaidPayments()) {
            if (settlementRepository.existsByPaymentId(payment.id())) {
                continue;   // 이미 정산된 결제 → 건너뜀(멱등)
            }
            long fee = SettlementPolicy.calculateFee(payment.amount());
            SettlementEntry entry = SettlementEntry.scheduled(
                    payment.id(),
                    payment.orderId(),
                    payment.pgTransactionId(),
                    payment.amount(),
                    fee,
                    settledDate);
            settlementRepository.save(entry);

            created++;
            totalGross += entry.getGrossAmount();
            totalFee += entry.getFee();
            totalNet += entry.getNetAmount();
        }

        return new SettlementRunResponse(created, totalGross, totalFee, totalNet);
    }

    /** 정산 항목 목록(페이지). 최신순은 컨트롤러의 기본 정렬로 처리. */
    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> getSettlements(Pageable pageable) {
        return PageResponse.from(
                settlementRepository.findAll(pageable).map(SettlementResponse::from));
    }

    /** 입금 확인 처리 → PAID_OUT. (실무라면 은행 입금 대사 후 호출. 여기선 수동 트리거.) */
    @Transactional
    public SettlementResponse payout(Long id) {
        SettlementEntry entry = settlementRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "정산 항목을 찾을 수 없습니다."));
        entry.markPaidOut();   // 상태머신 가드 — 이미 PAID_OUT이면 409. 변경은 더티 체킹으로 반영.
        return SettlementResponse.from(entry);
    }
}
