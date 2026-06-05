-- V3: 결제(payment) 도메인 추가.
--  · 주문(orders)은 다른 애그리거트 → order_id(ID 참조). 애그리거트 간 결합을 피하려 FK는 두지 않는다.
--  · status enum 값은 알파벳순(PaymentStatus + Hibernate ENUM DDL과 일치 → validate 통과).
--  · idempotency_key: 중복 결제 방지용 멱등키(unique).
-- (주문 흐름 변경 — orders.status를 PENDING/PAID로 — 은 P2의 V4에서 진행.)

CREATE TABLE `payment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `order_id` bigint NOT NULL,
  `amount` bigint NOT NULL,
  `status` enum('CANCELLED','FAILED','PAID','READY') COLLATE utf8mb4_unicode_ci NOT NULL,
  `method` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `pg_transaction_id` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `idempotency_key` varchar(80) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_payment_idempotency_key` (`idempotency_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
