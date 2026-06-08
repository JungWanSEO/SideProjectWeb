package com.commerce.api.settlement.service;

import com.commerce.api.global.common.PageResponse;
import com.commerce.api.payment.gateway.PaymentGateway;
import com.commerce.api.payment.gateway.PgSettlementRecord;
import com.commerce.api.payment.gateway.PgSettlementStatus;
import com.commerce.api.settlement.dto.MismatchResponse;
import com.commerce.api.settlement.dto.ReconciliationResult;
import com.commerce.api.settlement.entity.Mismatch;
import com.commerce.api.settlement.entity.MismatchType;
import com.commerce.api.settlement.entity.SettlementEntry;
import com.commerce.api.settlement.repository.MismatchRepository;
import com.commerce.api.settlement.repository.SettlementRepository;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대사(reconciliation) 서비스.
 *
 * <p>두 진실의 출처 — 우리 {@link SettlementEntry}와 PG 정산 리포트({@link PaymentGateway#fetchSettlements()}) —
 * 를 {@code pgTransactionId}로 매칭해 어긋남을 분류·기록한다. .NET으로 치면 두 컬렉션을 키로 outer-join 해
 * 교집합/차집합을 가르는 일.
 *
 * <p>PG 게이트웨이 포트를 직접 주입한다 — PG는 결제·정산이 공유하는 외부 인프라(특정 도메인 서비스가 아님).
 */
@Service
@RequiredArgsConstructor
public class ReconciliationService {

    private final SettlementRepository settlementRepository;
    private final MismatchRepository mismatchRepository;
    private final PaymentGateway paymentGateway;

    /**
     * 대사 실행 — 우리 정산 ↔ PG 리포트 전체를 거래키로 대조한다.
     *
     * <p>재실행은 <b>스냅샷</b> 의미: 이전 불일치를 비우고 현재 결과로 다시 채운다(중복 누적 방지).
     * 단순화: 실무는 일자별 윈도우로 대조하지만 여기선 전체를 본다(P1 정산 배치와 같은 단순화).
     */
    @Transactional
    public ReconciliationResult reconcile() {
        // 우리 측 / PG 측을 각각 거래키로 인덱싱(같은 키 중복은 이론상 없으나 안전하게 첫 값 유지)
        Map<String, SettlementEntry> ours = settlementRepository.findAll().stream()
                .collect(Collectors.toMap(SettlementEntry::getPgTransactionId, Function.identity(), (a, b) -> a));
        Map<String, PgSettlementRecord> pg = paymentGateway.fetchSettlements().stream()
                .collect(Collectors.toMap(PgSettlementRecord::pgTransactionId, Function.identity(), (a, b) -> a));

        mismatchRepository.deleteAllInBatch();   // 스냅샷: 직전 대사 결과 비우기

        int matched = 0, missingInPg = 0, missingInOurs = 0, amountMismatch = 0, statusMismatch = 0;

        Set<String> keys = new HashSet<>(ours.keySet());
        keys.addAll(pg.keySet());

        for (String key : keys) {
            SettlementEntry o = ours.get(key);
            PgSettlementRecord p = pg.get(key);

            if (o != null && p == null) {
                missingInPg++;
                save(key, MismatchType.MISSING_IN_PG, o.getGrossAmount(), null,
                        "우리 정산엔 있으나 PG 리포트에 없음(웹훅 유실/PG 누락 의심)");
            } else if (o == null && p != null) {
                missingInOurs++;
                save(key, MismatchType.MISSING_IN_OURS, null, p.amount(),
                        "PG 리포트엔 있으나 우리 정산 없음(정산 미실행/누락)");
            } else {   // 양쪽 존재
                if (p.status() == PgSettlementStatus.REFUNDED) {
                    statusMismatch++;
                    save(key, MismatchType.STATUS_MISMATCH, o.getGrossAmount(), p.amount(),
                            "PG는 환불됨이나 우리 정산은 미반영(상계 필요)");
                } else if (o.getGrossAmount() != p.amount()) {
                    amountMismatch++;
                    save(key, MismatchType.AMOUNT_MISMATCH, o.getGrossAmount(), p.amount(),
                            "금액 상이(수수료·부분취소 반영 차이)");
                } else {
                    matched++;   // 일치 건은 저장하지 않고 카운트만
                }
            }
        }

        int total = missingInPg + missingInOurs + amountMismatch + statusMismatch;
        return new ReconciliationResult(matched, missingInPg, missingInOurs, amountMismatch, statusMismatch, total);
    }

    private void save(String pgTransactionId, MismatchType type, Long ourAmount, Long pgAmount, String detail) {
        mismatchRepository.save(Mismatch.of(pgTransactionId, type, ourAmount, pgAmount, detail));
    }

    /** 불일치 항목 목록(페이지). */
    @Transactional(readOnly = true)
    public PageResponse<MismatchResponse> getMismatches(Pageable pageable) {
        return PageResponse.from(mismatchRepository.findAll(pageable).map(MismatchResponse::from));
    }
}
