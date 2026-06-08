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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 대사 불일치 항목 — 대사 잡이 찾아낸 "우리 ↔ PG 어긋남" 한 건.
 *
 * <p>이 행들이 "예외 큐"가 되어 사람이 처리한다(docs/payment-modern-architecture.md §3.3):
 * 생성 시 {@link MismatchStatus#OPEN}, 이후 {@code resolve}(상계·보정) 또는 {@code ignore}(오탐)로 종료.
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MismatchStatus status;    // 처리 상태(OPEN→RESOLVED/IGNORED)

    @Column(length = 255)
    private String resolutionNote;    // 처리 사유(처리 시 기록)

    private Mismatch(String pgTransactionId, MismatchType type, Long ourAmount, Long pgAmount, String detail) {
        this.pgTransactionId = pgTransactionId;
        this.type = type;
        this.ourAmount = ourAmount;
        this.pgAmount = pgAmount;
        this.detail = detail;
        this.status = MismatchStatus.OPEN;   // 생성 시점 = 미처리
    }

    public static Mismatch of(String pgTransactionId, MismatchType type,
                              Long ourAmount, Long pgAmount, String detail) {
        return new Mismatch(pgTransactionId, type, ourAmount, pgAmount, detail);
    }

    /** 처리 완료 → RESOLVED. (OPEN 상태에서만 가능) */
    public void resolve(String note) {
        requireOpen();
        this.status = MismatchStatus.RESOLVED;
        this.resolutionNote = note;
    }

    /** 무시 → IGNORED. (OPEN 상태에서만 가능) */
    public void ignore(String note) {
        requireOpen();
        this.status = MismatchStatus.IGNORED;
        this.resolutionNote = note;
    }

    /** 이미 처리된(종료된) 항목의 재처리를 막는다(상태머신 가드). */
    private void requireOpen() {
        if (this.status != MismatchStatus.OPEN) {
            throw new BusinessException(HttpStatus.CONFLICT,
                    "이미 처리된 불일치입니다. (현재: " + this.status + ")");
        }
    }
}
