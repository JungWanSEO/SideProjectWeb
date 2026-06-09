-- V13: 대사 불일치에 PG(provider) 추가 — 다중 PG MPG-2.
--  · 불일치를 어느 PG의 거래인지로 분류·필터·표시하기 위함(우리 SettlementEntry는 MPG-3에서 이미 provider 보유).
--  · 거래 ID 프리픽스(KAKAO-)와 provider(KAKAOPAY)가 다를 수 있어 프리픽스 파싱이 아니라 명시 컬럼으로 둔다.
--  · NOT NULL이지만 기존 행(있다면)을 위해 DEFAULT 'TOSS'로 백필(앱은 항상 명시적으로 채운다).
--  · pg_transaction_id 옆에 둔다(둘 다 거래 식별 메타).

ALTER TABLE `mismatch`
  ADD COLUMN `provider` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TOSS' AFTER `pg_transaction_id`;
