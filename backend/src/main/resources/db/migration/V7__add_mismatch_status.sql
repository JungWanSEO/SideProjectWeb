-- V7: 대사 불일치에 처리 상태(예외 큐 생명주기) 추가.
--  · status: OPEN(미처리) → RESOLVED(상계·보정) / IGNORED(오탐). 재대사는 OPEN만 스냅샷, 처리된 건은 보존.
--  · status enum 값은 알파벳순(MismatchStatus + Hibernate ENUM DDL과 일치 → validate 통과).
--  · 기존 행 대비 안전하게 DEFAULT 'OPEN'(엔티티는 생성 시 OPEN으로 채움 — 신규 행엔 default 미사용).

ALTER TABLE `mismatch`
  ADD COLUMN `status` enum('IGNORED','OPEN','RESOLVED') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OPEN' AFTER `detail`,
  ADD COLUMN `resolution_note` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL AFTER `status`;
