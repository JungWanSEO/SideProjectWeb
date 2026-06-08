-- V6: 대사(reconciliation) 불일치 기록 테이블.
--  · 대사 잡이 우리 정산(settlement_entry) ↔ PG 정산 리포트를 pg_transaction_id로 대조해 어긋난 건을 남긴다.
--  · our_amount/pg_amount는 한쪽에만 있는 경우(MISSING_*) NULL 허용.
--  · type enum 값은 알파벳순(MismatchType + Hibernate ENUM DDL과 일치 → validate 통과).
--  · 별도 unique 제약 없음 — 재실행은 서비스에서 전체 삭제 후 재삽입(스냅샷)으로 처리.

CREATE TABLE `mismatch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `pg_transaction_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` enum('AMOUNT_MISMATCH','MISSING_IN_OURS','MISSING_IN_PG','STATUS_MISMATCH') COLLATE utf8mb4_unicode_ci NOT NULL,
  `our_amount` bigint DEFAULT NULL,
  `pg_amount` bigint DEFAULT NULL,
  `detail` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
