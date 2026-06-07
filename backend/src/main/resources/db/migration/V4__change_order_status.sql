-- V4: 주문 상태머신을 결제 흐름에 맞게 변경.
--  · ORDERED(주문=완료) → PENDING(결제 대기) / PAID(결제 완료)로 분리. 재고 차감 시점이 주문→결제 승인으로 이동.
--  · 기존 'ORDERED' 행은 '완료된 주문' = 결제됨으로 보고 'PAID'로 이전.
--  · 최종 enum 값은 알파벳순(OrderStatus + Hibernate ENUM DDL과 일치 → validate 통과).

-- 1) 기존/신규 값을 모두 허용하도록 임시 확장 (ORDERED 행을 옮기기 위해)
ALTER TABLE `orders`
  MODIFY COLUMN `status` enum('CANCELLED','ORDERED','PAID','PENDING')
      COLLATE utf8mb4_unicode_ci NOT NULL;

-- 2) 과거 완료 주문(ORDERED)을 PAID로 이전
UPDATE `orders` SET `status` = 'PAID' WHERE `status` = 'ORDERED';

-- 3) 최종 enum으로 좁힘 (알파벳순)
ALTER TABLE `orders`
  MODIFY COLUMN `status` enum('CANCELLED','PAID','PENDING')
      COLLATE utf8mb4_unicode_ci NOT NULL;
