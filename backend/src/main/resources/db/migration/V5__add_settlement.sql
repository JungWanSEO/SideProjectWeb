-- V5: 정산(settlement) 도메인 추가.
--  · 결제(payment)·주문(orders)은 다른 애그리거트 → payment_id/order_id(ID 참조). FK는 두지 않는다(애그리거트 간 결합 회피).
--  · pg_transaction_id: 대사(reconciliation)의 조인 키 — P2에서 PG 정산 리포트와 매칭한다.
--  · net_amount: 실입금 = gross_amount - fee. "매출 ≠ 결제액"을 1급 컬럼으로.
--  · status enum 값은 알파벳순(PAID_OUT, SCHEDULED) — SettlementStatus + Hibernate ENUM DDL과 일치 → validate 통과.
--  · payment_id UNIQUE: 한 결제당 정산 항목 1개 → 정산 배치 재실행에도 중복 생성 방지(멱등).

CREATE TABLE `settlement_entry` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `payment_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `pg_transaction_id` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `gross_amount` bigint NOT NULL,
  `fee` bigint NOT NULL,
  `net_amount` bigint NOT NULL,
  `status` enum('PAID_OUT','SCHEDULED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `settled_date` date NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_settlement_payment_id` (`payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
