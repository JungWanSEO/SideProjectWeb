package com.commerce.api.payment.entity;

/**
 * 결제 상태 (상태머신).
 *
 * READY(승인 전) → PAID(승인 성공) / FAILED(승인 실패), PAID → CANCELLED(환불).
 * ⚠️ MySQL ENUM 매핑 시 Hibernate는 값을 알파벳순으로 생성하므로, 마이그레이션(V3)의
 *    enum 값 집합도 알파벳순으로 맞춰야 validate가 통과한다. (값 추가 시 주의)
 */
public enum PaymentStatus {
    READY,      // 결제 시도 레코드 생성됨 (승인 전)
    PAID,       // PG 승인 성공
    FAILED,     // PG 승인 실패
    CANCELLED   // 결제 취소/환불
}
