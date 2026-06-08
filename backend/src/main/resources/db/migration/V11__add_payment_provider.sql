-- V11: 결제를 처리한 PG(provider) 컬럼.
--  · 다중 PG(토스/카카오…) 도입 — 어느 PG로 승인했는지 저장해야 환불을 같은 PG로 라우팅하고, 정산·대사도 PG를 구분한다.
--  · NOT NULL이지만 기존 행(있다면)을 위해 DEFAULT 'TOSS'로 백필(앱은 항상 명시적으로 채운다).
--  · method 옆에 둔다(둘 다 결제 수단 메타).

ALTER TABLE `payment`
  ADD COLUMN `provider` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TOSS' AFTER `method`;
