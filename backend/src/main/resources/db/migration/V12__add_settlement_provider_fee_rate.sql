-- V12: 정산 항목에 PG(provider)와 적용 수수료율(fee_rate) 추가 — 다중 PG MPG-3.
--  · provider: 정산 대상 결제를 처리한 PG. PG별 수수료율·집계의 키(payment.provider와 같은 표기).
--  · fee_rate: 정산 시점에 적용한 수수료율 스냅샷(예: 0.025). 요율이 나중에 바뀌어도 "그때 몇 %"를 보존.
--    돈(원)이 아니라 비율이고 합산하지 않으므로 double로 둔다(money는 여전히 bigint).
--  · 기존 행(있다면)은 단일 2.5% TOSS 세계에서 만들어졌으므로 provider='TOSS', fee_rate=0.025로 백필.
--    (앱은 항상 명시적으로 채운다 — DEFAULT는 안전 백필용. validate는 default를 검사하지 않음.)
--  · pg_transaction_id 옆에 둔다(둘 다 PG 메타).

ALTER TABLE `settlement_entry`
  ADD COLUMN `provider` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TOSS' AFTER `pg_transaction_id`,
  ADD COLUMN `fee_rate` double NOT NULL DEFAULT 0.025 AFTER `fee`;
