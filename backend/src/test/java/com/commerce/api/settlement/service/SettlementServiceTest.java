package com.commerce.api.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.payment.dto.PaymentResponse;
import com.commerce.api.payment.entity.PaymentStatus;
import com.commerce.api.payment.service.PaymentService;
import com.commerce.api.settlement.dto.SettlementResponse;
import com.commerce.api.settlement.dto.SettlementRunResponse;
import com.commerce.api.settlement.entity.SettlementEntry;
import com.commerce.api.settlement.entity.SettlementStatus;
import com.commerce.api.settlement.repository.SettlementRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * SettlementService 단위 테스트 — 정산 배치(수수료·실입금 계산, 멱등) / 입금 확인(상태머신).
 */
@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private SettlementService settlementService;

    private PaymentResponse paidPayment(Long id, Long orderId, long amount) {
        return paidPayment(id, orderId, amount, "TOSS");
    }

    private PaymentResponse paidPayment(Long id, Long orderId, long amount, String provider) {
        return new PaymentResponse(id, orderId, amount, PaymentStatus.PAID,
                "MOCK_CARD", provider, "MOCK-tx-" + id, LocalDateTime.now());
    }

    @Test
    @DisplayName("정산 배치 - PAID 결제마다 수수료(TOSS 2.5%)를 떼고 실입금을 계산해 SCHEDULED 항목 생성")
    void run_createsEntriesWithFeeAndNet() {
        given(paymentService.getPaidPayments()).willReturn(List.of(
                paidPayment(1L, 11L, 30000L),
                paidPayment(2L, 12L, 10000L)));
        given(settlementRepository.existsByPaymentId(anyLong())).willReturn(false);
        given(settlementRepository.save(any(SettlementEntry.class))).willAnswer(inv -> inv.getArgument(0));

        SettlementRunResponse summary = settlementService.run();

        // 요약: 2건 생성, 합계 = 결제액 40,000 / 수수료 1,000(=750+250) / 실입금 39,000
        assertThat(summary.createdCount()).isEqualTo(2);
        assertThat(summary.totalGrossAmount()).isEqualTo(40000L);
        assertThat(summary.totalFee()).isEqualTo(1000L);
        assertThat(summary.totalNetAmount()).isEqualTo(39000L);

        // 저장된 항목 검증: 첫 결제 30,000 → 수수료 750, 실입금 29,250, SCHEDULED, 입금일 = T+2
        ArgumentCaptor<SettlementEntry> captor = ArgumentCaptor.forClass(SettlementEntry.class);
        verify(settlementRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        SettlementEntry first = captor.getAllValues().get(0);
        assertThat(first.getPaymentId()).isEqualTo(1L);
        assertThat(first.getPgTransactionId()).isEqualTo("MOCK-tx-1");   // 대사 조인 키 보존
        assertThat(first.getProvider()).isEqualTo("TOSS");              // PG 보존(MPG-3)
        assertThat(first.getGrossAmount()).isEqualTo(30000L);
        assertThat(first.getFee()).isEqualTo(750L);
        assertThat(first.getFeeRate()).isEqualTo(0.025);               // 적용 요율 스냅샷
        assertThat(first.getNetAmount()).isEqualTo(29250L);             // 매출 ≠ 결제액
        assertThat(first.getStatus()).isEqualTo(SettlementStatus.SCHEDULED);
        assertThat(first.getSettledDate()).isEqualTo(LocalDate.now().plusDays(2));
    }

    @Test
    @DisplayName("정산 배치 - PG마다 요율이 다르다(TOSS 2.5% vs KAKAOPAY 2.8%) → 같은 금액도 수수료가 갈리고 PG별로 집계된다")
    void run_perProviderFeeRatesAndBreakdown() {
        given(paymentService.getPaidPayments()).willReturn(List.of(
                paidPayment(1L, 11L, 10000L, "TOSS"),       // 2.5% → 250
                paidPayment(2L, 12L, 10000L, "KAKAOPAY"),   // 2.8% → 280 (같은 금액, 다른 수수료)
                paidPayment(3L, 13L, 20000L, "KAKAOPAY"))); // 2.8% → 560
        given(settlementRepository.existsByPaymentId(anyLong())).willReturn(false);
        given(settlementRepository.save(any(SettlementEntry.class))).willAnswer(inv -> inv.getArgument(0));

        SettlementRunResponse summary = settlementService.run();

        // 합계: gross 40,000 / fee 1,090(=250+280+560) / net 38,910
        assertThat(summary.createdCount()).isEqualTo(3);
        assertThat(summary.totalGrossAmount()).isEqualTo(40000L);
        assertThat(summary.totalFee()).isEqualTo(1090L);
        assertThat(summary.totalNetAmount()).isEqualTo(38910L);

        // PG별 분해 — 삽입 순서(TOSS 먼저, KAKAOPAY 다음)
        assertThat(summary.byProvider()).hasSize(2);
        SettlementRunResponse.ProviderBreakdown toss = summary.byProvider().get(0);
        assertThat(toss.provider()).isEqualTo("TOSS");
        assertThat(toss.feeRate()).isEqualTo(0.025);
        assertThat(toss.count()).isEqualTo(1);
        assertThat(toss.grossAmount()).isEqualTo(10000L);
        assertThat(toss.fee()).isEqualTo(250L);
        assertThat(toss.netAmount()).isEqualTo(9750L);

        SettlementRunResponse.ProviderBreakdown kakao = summary.byProvider().get(1);
        assertThat(kakao.provider()).isEqualTo("KAKAOPAY");
        assertThat(kakao.feeRate()).isEqualTo(0.028);
        assertThat(kakao.count()).isEqualTo(2);
        assertThat(kakao.grossAmount()).isEqualTo(30000L);
        assertThat(kakao.fee()).isEqualTo(840L);                       // 280 + 560
        assertThat(kakao.netAmount()).isEqualTo(29160L);
    }

    @Test
    @DisplayName("정산 배치 - 요율표에 없는 PG는 폴백 요율(3.0%)이 적용된다")
    void run_unknownProviderUsesFallbackRate() {
        given(paymentService.getPaidPayments()).willReturn(List.of(
                paidPayment(1L, 11L, 10000L, "PAYPAL")));   // 요율표 미등록 → DEFAULT_FEE_RATE 3.0%
        given(settlementRepository.existsByPaymentId(anyLong())).willReturn(false);
        given(settlementRepository.save(any(SettlementEntry.class))).willAnswer(inv -> inv.getArgument(0));

        SettlementRunResponse summary = settlementService.run();

        assertThat(summary.totalFee()).isEqualTo(300L);                // 10,000 × 3.0%
        assertThat(summary.byProvider().get(0).feeRate()).isEqualTo(0.030);
    }

    @Test
    @DisplayName("정산 배치 - 이미 정산된 결제는 건너뜀(멱등) — 재실행해도 중복 생성 없음")
    void run_idempotentSkipsAlreadySettled() {
        given(paymentService.getPaidPayments()).willReturn(List.of(paidPayment(1L, 11L, 30000L)));
        given(settlementRepository.existsByPaymentId(1L)).willReturn(true);   // 이미 정산됨

        SettlementRunResponse summary = settlementService.run();

        assertThat(summary.createdCount()).isZero();
        verify(settlementRepository, never()).save(any());
    }

    @Test
    @DisplayName("입금 확인 - SCHEDULED → PAID_OUT")
    void payout_marksPaidOut() {
        SettlementEntry entry = SettlementEntry.scheduled(
                1L, 11L, "MOCK-tx-1", "TOSS", 30000L, 750L, 0.025, LocalDate.now().plusDays(2));
        given(settlementRepository.findById(1L)).willReturn(Optional.of(entry));

        SettlementResponse response = settlementService.payout(1L);

        assertThat(response.status()).isEqualTo(SettlementStatus.PAID_OUT);
    }

    @Test
    @DisplayName("입금 확인 - 없는 정산 항목이면 404")
    void payout_notFound() {
        given(settlementRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> settlementService.payout(99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("정산 항목을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("입금 확인 - 이미 PAID_OUT이면 409(상태머신 가드)")
    void payout_alreadyPaidOut() {
        SettlementEntry entry = SettlementEntry.scheduled(
                1L, 11L, "MOCK-tx-1", "TOSS", 30000L, 750L, 0.025, LocalDate.now().plusDays(2));
        entry.markPaidOut();   // 이미 입금 처리됨
        given(settlementRepository.findById(1L)).willReturn(Optional.of(entry));

        assertThatThrownBy(() -> settlementService.payout(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("정산 상태 전이가 올바르지 않습니다");
    }
}
