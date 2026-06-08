package com.commerce.api.settlement.entity;

import com.commerce.api.global.common.BaseEntity;
import com.commerce.api.global.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 정산 항목 (애그리거트 루트).
 *
 * <p>"결제 한 건이 며칠 뒤 수수료를 떼고 얼마가 입금되는가"를 1급으로 모델링한다.
 * 결제(Payment)와 분리한 이유: 생명주기·관심사가 다르다(거래 승인 ↔ 자금 입금). 결제 엔티티에
 * {@code fee}·{@code settledDate}를 욱여넣으면 결제 도메인이 정산 걱정까지 떠안아 오염된다
 * (docs/payment-modern-architecture.md §3.5).
 *
 * <p>다른 애그리거트(결제·주문)는 객체 연관 대신 ID로 참조한다(architecture.md §11).
 *
 * <p><b>핵심: 매출 ≠ 결제액.</b> 결제 10,000원이라도 PG 수수료를 떼면 실입금(netAmount)은 9,750원처럼
 * 달라진다. 이 차이를 {@code grossAmount}/{@code fee}/{@code netAmount} 세 필드로 명시한다.
 */
@Getter
@Entity
@Table(name = "settlement_entry")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;          // 정산 대상 결제 (다른 애그리거트 → ID 참조)

    @Column(nullable = false)
    private Long orderId;            // 주문 (조회 편의를 위해 함께 보관 — 역시 ID 참조)

    @Column(nullable = false, length = 100)
    private String pgTransactionId;  // ★ 대사(reconciliation)의 조인 키 — P2에서 PG 리포트와 매칭한다

    @Column(nullable = false)
    private long grossAmount;        // 결제액(원) = Payment.amount

    @Column(nullable = false)
    private long fee;                // PG 수수료(원)

    @Column(nullable = false)
    private long netAmount;          // 실입금(원) = grossAmount - fee  ← "매출 ≠ 결제액"의 핵심

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(nullable = false)
    private LocalDate settledDate;   // 입금(정산) 예정/완료일 (T+N)

    private SettlementEntry(Long paymentId, Long orderId, String pgTransactionId,
                            long grossAmount, long fee, LocalDate settledDate) {
        this.paymentId = paymentId;
        this.orderId = orderId;
        this.pgTransactionId = pgTransactionId;
        this.grossAmount = grossAmount;
        this.fee = fee;
        this.netAmount = grossAmount - fee;          // 실입금은 파생값 — 엔티티가 스스로 계산해 일관성 보장
        this.settledDate = settledDate;
        this.status = SettlementStatus.SCHEDULED;    // 생성 시점 = 입금 전(예정)
    }

    /** 정산 예정 항목 생성 (수수료는 정책이 계산해 넘겨준다). */
    public static SettlementEntry scheduled(Long paymentId, Long orderId, String pgTransactionId,
                                            long grossAmount, long fee, LocalDate settledDate) {
        return new SettlementEntry(paymentId, orderId, pgTransactionId, grossAmount, fee, settledDate);
    }

    /** 입금 확인 → PAID_OUT. (SCHEDULED 상태에서만 가능) */
    public void markPaidOut() {
        if (this.status != SettlementStatus.SCHEDULED) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "정산 상태 전이가 올바르지 않습니다. (현재: " + this.status + ", 기대: " + SettlementStatus.SCHEDULED + ")");
        }
        this.status = SettlementStatus.PAID_OUT;
    }
}
