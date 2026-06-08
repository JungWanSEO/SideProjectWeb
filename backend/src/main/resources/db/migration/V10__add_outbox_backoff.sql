-- V10: 아웃박스 지수 백오프 컬럼.
--  · next_attempt_at: 발행 실패 시 "다음 재시도 가능 시각"을 미래로 밀어 헛된 재시도 폭주를 막는다(NULL=즉시 대상).
--  · 폴러 후보 스캔이 status='PENDING' AND (next_attempt_at IS NULL OR next_attempt_at <= now)로 바뀌므로
--    조회 인덱스를 (status, next_attempt_at)로 보강한다(기존 idx_outbox_status 대체).
--  · 행별 잠금 클레임(FOR UPDATE SKIP LOCKED)은 스키마 변경 없이 쿼리 레벨에서 처리(폴러 스케일아웃 안전).

ALTER TABLE `outbox_event` ADD COLUMN `next_attempt_at` datetime(6) DEFAULT NULL AFTER `retry_count`;

ALTER TABLE `outbox_event` DROP INDEX `idx_outbox_status`;
ALTER TABLE `outbox_event` ADD KEY `idx_outbox_dispatch` (`status`, `next_attempt_at`);
