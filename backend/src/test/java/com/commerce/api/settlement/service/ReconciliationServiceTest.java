package com.commerce.api.settlement.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.payment.gateway.PaymentGateway;
import com.commerce.api.payment.gateway.PgSettlementRecord;
import com.commerce.api.payment.gateway.PgSettlementStatus;
import com.commerce.api.settlement.dto.MismatchResponse;
import com.commerce.api.settlement.dto.ReconciliationResult;
import com.commerce.api.settlement.entity.Mismatch;
import com.commerce.api.settlement.entity.MismatchStatus;
import com.commerce.api.settlement.entity.MismatchType;
import com.commerce.api.settlement.entity.SettlementEntry;
import com.commerce.api.settlement.repository.MismatchRepository;
import com.commerce.api.settlement.repository.SettlementRepository;
import java.time.LocalDate;
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
 * ReconciliationService 단위 테스트 — 대조 5분류 + 처리(resolve/ignore) 생명주기.
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

    // ---------- 대조(분류) ----------

    @Test
    @DisplayName("일치 - 양측 존재·금액·상태 동일이면 MATCHED, 불일치 0 (OPEN 스냅샷만 비움)")
    void matched() {
        given(settlementRepository.findAll()).willReturn(List.of(our("tx1", 10000)));
        given(paymentGateway.fetchSettlements()).willReturn(List.of(pg("tx1", 10000, PgSettlementStatus.PAID)));

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.matched()).isEqualTo(1);
        assertThat(r.totalMismatches()).isZero();
        verify(mismatchRepository).deleteByStatus(MismatchStatus.OPEN);   // OPEN만 비움(처리된 건 보존)
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
        assertThat(m.getStatus()).isEqualTo(MismatchStatus.OPEN);   // 새 불일치는 OPEN
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

    @Test
    @DisplayName("재대사 - 이미 처리(RESOLVED/IGNORED)된 거래키는 다시 OPEN으로 안 만들고 alreadyHandled로 집계")
    void reconcile_skipsAlreadyHandled() {
        given(settlementRepository.findAll()).willReturn(List.of(our("tx1", 10000)));  // PG에 없으니 MISSING_IN_PG 후보
        given(paymentGateway.fetchSettlements()).willReturn(List.of());
        Mismatch handled = Mismatch.of("tx1", MismatchType.MISSING_IN_PG, 10000L, null, "x");
        handled.resolve("이미 수기 처리함");
        given(mismatchRepository.findByStatusIn(any())).willReturn(List.of(handled));

        ReconciliationResult r = reconciliationService.reconcile();

        assertThat(r.alreadyHandled()).isEqualTo(1);
        assertThat(r.missingInPg()).isZero();
        assertThat(r.totalMismatches()).isZero();
        verify(mismatchRepository, never()).save(any());   // 다시 OPEN으로 안 만듦
    }

    // ---------- 처리(resolve / ignore) ----------

    @Test
    @DisplayName("처리(resolve) - OPEN → RESOLVED, 사유 기록")
    void resolve_marksResolved() {
        Mismatch m = Mismatch.of("tx1", MismatchType.STATUS_MISMATCH, 10000L, 10000L, "x");
        given(mismatchRepository.findById(1L)).willReturn(Optional.of(m));

        MismatchResponse res = reconciliationService.resolve(1L, "수기 상계 처리");

        assertThat(res.status()).isEqualTo(MismatchStatus.RESOLVED);
        assertThat(res.resolutionNote()).isEqualTo("수기 상계 처리");
    }

    @Test
    @DisplayName("무시(ignore) - OPEN → IGNORED")
    void ignore_marksIgnored() {
        Mismatch m = Mismatch.of("tx1", MismatchType.AMOUNT_MISMATCH, 10000L, 9000L, "x");
        given(mismatchRepository.findById(1L)).willReturn(Optional.of(m));

        MismatchResponse res = reconciliationService.ignore(1L, "오탐");

        assertThat(res.status()).isEqualTo(MismatchStatus.IGNORED);
    }

    @Test
    @DisplayName("처리 - 없는 항목이면 404")
    void resolve_notFound() {
        given(mismatchRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reconciliationService.resolve(99L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("불일치 항목을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("처리 - 이미 종료된 불일치면 409(상태머신 가드)")
    void resolve_alreadyTerminal() {
        Mismatch m = Mismatch.of("tx1", MismatchType.STATUS_MISMATCH, 10000L, 10000L, "x");
        m.resolve("먼저 처리됨");
        given(mismatchRepository.findById(1L)).willReturn(Optional.of(m));

        assertThatThrownBy(() -> reconciliationService.resolve(1L, "다시"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 처리된 불일치");
    }

    private Mismatch captureSaved() {
        ArgumentCaptor<Mismatch> captor = ArgumentCaptor.forClass(Mismatch.class);
        verify(mismatchRepository).save(captor.capture());
        return captor.getValue();
    }
}
