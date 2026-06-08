-- V9: 알림 로그 테이블 — 아웃박스 이벤트 소비 결과(모의 알림).
--  · event_id UNIQUE: 멱등 소비 보장(at-least-once라 같은 이벤트 중복 도착 → 두 번째 INSERT 차단).

CREATE TABLE `notification_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `event_id` bigint NOT NULL,
  `type` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_event_id` (`event_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
