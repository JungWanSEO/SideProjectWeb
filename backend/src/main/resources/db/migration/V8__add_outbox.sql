-- V8: 트랜잭셔널 아웃박스 테이블.
--  · 도메인 상태 변경과 같은 트랜잭션에 이벤트를 INSERT(원자성) → 폴러가 나중에 발행(at-least-once).
--  · payload: 학습용 작은 JSON이라 varchar(1000)로 충분.
--  · status enum 값은 알파벳순(OutboxStatus + Hibernate ENUM DDL과 일치 → validate 통과).
--  · idx_outbox_status: 폴러의 "PENDING 생성순 조회"를 위한 인덱스.

CREATE TABLE `outbox_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `event_type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `aggregate_type` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `aggregate_id` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` enum('FAILED','PENDING','PUBLISHED') COLLATE utf8mb4_unicode_ci NOT NULL,
  `retry_count` int NOT NULL DEFAULT 0,
  `published_at` datetime(6) DEFAULT NULL,
  `last_error` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_outbox_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
