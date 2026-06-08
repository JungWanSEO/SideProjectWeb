package com.commerce.api.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.commerce.api.payment.gateway.PaymentGateway;
import com.commerce.api.payment.gateway.PgSettlementRecord;
import com.commerce.api.payment.gateway.PgSettlementStatus;
import com.commerce.api.settlement.dto.ReconciliationResult;
import com.commerce.api.settlement.entity.Mismatch;
import com.commerce.api.settlement.entity.MismatchType;
import com.commerce.api.settlement.entity.SettlementEntry;
import com.commerce.api.settlement.repository.MismatchRepository;
import com.commerce.api.settlement.repository.SettlementRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * ReconciliationService 단위 테스트 — 우리 정산 ↔ PG 리포트 대조의 5가지 분류.
 * (양측을 목으로 직접 구성하므로 자연 발생이 어려운 케이스도 그대로 검증 가능.)
 */
@ExtendWith(MockitoExtension.class)
class ReconciliationServiceTest {

    @Mock
    private SettlementRepository settlementRepository;
    @Mock
    private MismatchRepository mismatchRepository;
    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private ReconciliationService reconciliationService;

    private SettlementEntry our(String pgTx, long gross) {
        return SettlementEntry.scheduled(1L, 1L, pgTx, gross, 0L, LocalDate.now().plusDays(2));
    }

    private PgSettlementRecord pg(String pgTx, long amount, PgSettlementStatus status) {
        return new PgSettlementRecord(pgTx, amount, status);
    }

    @Test
    @DisplayName("일치 - 양측 존재·금액·상태 동일이면 MATCHED, 불일치 0 (Mismatch 미저장)")
    void matched() {
        given(settlementRepository.findAll()).willReturn(List.of(our("tx1", 10000)));
        given(paymentGateway.fetchSettlements()).willReturn(List.of(pg("tx1", 10000, PgSettlementStatus.PAID)));

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.matched()).isEqualTo(1);
        assertThat(r.totalMismatches()).isZero();
        verify(mismatchRepository).deleteAllInBatch();   // 스냅샷 재생성
        verify(mismatchRepository, never()).save(any());
    }

    @Test
    @DisplayName("MISSING_IN_PG - 우리엔 있고 PG엔 없음")
    void missingInPg() {
        given(settlementRepository.findAll()).willReturn(List.of(our("tx1", 10000)));
        given(paymentGateway.fetchSettlements()).willReturn(List.of());

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.missingInPg()).isEqualTo(1);
        assertThat(r.totalMismatches()).isEqualTo(1);
        Mismatch m = captureSaved();
        assertThat(m.getType()).isEqualTo(MismatchType.MISSING_IN_PG);
        assertThat(m.getOurAmount()).isEqualTo(10000L);
        assertThat(m.getPgAmount()).isNull();
    }

    @Test
    @DisplayName("MISSING_IN_OURS - PG엔 있고 우리엔 없음")
    void missingInOurs() {
        given(settlementRepository.findAll()).willReturn(List.of());
        given(paymentGateway.fetchSettlements()).willReturn(List.of(pg("tx1", 10000, PgSettlementStatus.PAID)));

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.missingInOurs()).isEqualTo(1);
        Mismatch m = captureSaved();
        assertThat(m.getType()).isEqualTo(MismatchType.MISSING_IN_OURS);
        assertThat(m.getOurAmount()).isNull();
        assertThat(m.getPgAmount()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("AMOUNT_MISMATCH - 양측 있으나 금액 상이")
    void amountMismatch() {
        given(settlementRepository.findAll()).willReturn(List.of(our("tx1", 10000)));
        given(paymentGateway.fetchSettlements()).willReturn(List.of(pg("tx1", 9000, PgSettlementStatus.PAID)));

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.amountMismatch()).isEqualTo(1);
        Mismatch m = captureSaved();
        assertThat(m.getType()).isEqualTo(MismatchType.AMOUNT_MISMATCH);
        assertThat(m.getOurAmount()).isEqualTo(10000L);
        assertThat(m.getPgAmount()).isEqualTo(9000L);
    }

    @Test
    @DisplayName("STATUS_MISMATCH - 우리는 정산했는데 PG는 환불됨(정산 후 취소분)")
    void statusMismatch() {
        given(settlementRepository.findAll()).willReturn(List.of(our("tx1", 10000)));
        given(paymentGateway.fetchSettlements()).willReturn(List.of(pg("tx1", 10000, PgSettlementStatus.REFUNDED)));

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.statusMismatch()).isEqualTo(1);
        Mismatch m = captureSaved();
        assertThat(m.getType()).isEqualTo(MismatchType.STATUS_MISMATCH);
    }

    @Test
    @DisplayName("혼합 - 일치/3종 불일치를 한 번에 정확히 분류·집계")
    void mixed() {
        given(settlementRepository.findAll()).willReturn(List.of(
                our("tx1", 10000),   // 일치
                our("tx2", 5000),    // STATUS_MISMATCH (아래 PG가 REFUNDED)
                our("tx3", 3000)));  // MISSING_IN_PG (PG에 없음)
        given(paymentGateway.fetchSettlements()).willReturn(List.of(
                pg("tx1", 10000, PgSettlementStatus.PAID),
                pg("tx2", 5000, PgSettlementStatus.REFUNDED),
                pg("tx4", 4000, PgSettlementStatus.PAID)));   // MISSING_IN_OURS (우리에 없음)

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.matched()).isEqualTo(1);
        assertThat(r.statusMismatch()).isEqualTo(1);
        assertThat(r.missingInPg()).isEqualTo(1);
        assertThat(r.missingInOurs()).isEqualTo(1);
        assertThat(r.amountMismatch()).isZero();
        assertThat(r.totalMismatches()).isEqualTo(3);
        verify(mismatchRepository, times(3)).save(any(Mismatch.class));
    }

    private Mismatch captureSaved() {
        ArgumentCaptor<Mismatch> captor = ArgumentCaptor.forClass(Mismatch.class);
        verify(mismatchRepository).save(captor.capture());
        return captor.getValue();
    }
}
