package com.commerce.api.settlement.entity;

/**
 * 대사 불일치의 처리 상태 — "예외 큐"의 생명주기.
 *
 * <pre>
 *   OPEN      미처리(대사가 막 찾아낸 상태)
 *    ├─▶ RESOLVED  처리 완료(상계·보정함)
 *    └─▶ IGNORED   무시(오탐·허용 가능으로 판단)
 * </pre>
 *
 * <p>재대사 시 OPEN은 새로 스냅샷되지만 RESOLVED/IGNORED는 보존되고, 그 거래키는
 * 다시 OPEN으로 만들지 않는다(사람의 처리 결정을 존중).
 *
 * ⚠️ enum 값은 알파벳순(IGNORED, OPEN, RESOLVED) — Hibernate ENUM DDL ↔ Flyway 일치(validate).
 */
public enum MismatchStatus {
    IGNORED,
    OPEN,
    RESOLVED
}
