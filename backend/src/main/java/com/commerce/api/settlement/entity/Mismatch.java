package com.commerce.api.settlement.entity;

import com.commerce.api.global.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대사 불일치 항목 — 대사 잡이 찾아낸 "우리 ↔ PG 어긋남" 한 건.
 *
 * <p>실무라면 이 행들이 "예외 큐"가 되어 자동 보정 또는 사람이 처리한다
 * (docs/payment-modern-architecture.md §3.3). 여기선 기록·조회까지 모델링한다.
 *
 * <p>{@code ourAmount}/{@code pgAmount}는 한쪽에만 있는 경우 null이 될 수 있다
 * (MISSING_IN_PG면 pgAmount=null, MISSING_IN_OURS면 ourAmount=null).
 */
@Getter
@Entity
@Table(name = "mismatch")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Mismatch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String pgTransactionId;   // 조인 키 — 어느 거래가 어긋났나

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MismatchType type;

    private Long ourAmount;           // 우리 기록 금액(없으면 null)

    private Long pgAmount;            // PG 리포트 금액(없으면 null)

    @Column(length = 255)
    private String detail;            // 사람이 읽을 설명

    private Mismatch(String pgTransactionId, MismatchType type, Long ourAmount, Long pgAmount, String detail) {
        this.pgTransactionId = pgTransactionId;
        this.type = type;
        this.ourAmount = ourAmount;
        this.pgAmount = pgAmount;
        this.detail = detail;
    }

    public static Mismatch of(String pgTransactionId, MismatchType type,
                              Long ourAmount, Long pgAmount, String detail) {
        return new Mismatch(pgTransactionId, type, ourAmount, pgAmount, detail);
    }
}
