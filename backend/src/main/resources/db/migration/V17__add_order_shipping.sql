-- V17: 주문 배송지 스냅샷 — Phase 1 #4 Step 3.
--  · orders에 배송지 컬럼(@Embedded ShippingInfo)을 추가한다. 체크아웃 시 주소록에서 골라 복사(스냅샷).
--  · 모두 nullable: 명시적 주문 생성(POST /api/orders)이나 기존 주문은 배송지가 없을 수 있다.
--  · 주소록(address)과 FK로 묶지 않는다 — 스냅샷이라 주소록이 바뀌어도 과거 주문은 불변.

ALTER TABLE `orders`
  ADD COLUMN `recipient`     varchar(50)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `phone`         varchar(30)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `zipcode`       varchar(10)  COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `address1`      varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `address2`      varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  ADD COLUMN `delivery_memo` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL;
