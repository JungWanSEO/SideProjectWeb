-- V14: 상품 대표 이미지 URL 추가 — Phase 1 #1(상품 이미지).
--  · 단일 대표 이미지 1장만(갤러리는 후속 — 플랜의 '과투자 금지').
--  · nullable: 이미지 없는/기존 상품 허용. 비어 있으면 FE가 상품 id 기반 결정적 placeholder로 폴백한다.
--  · 로컬 정적 자산 경로('/products/3.svg')나 외부 URL 모두 담을 수 있게 varchar(500).
--  · description 옆(표시용 콘텐츠 묶음)에 둔다.

ALTER TABLE `product`
  ADD COLUMN `image_url` varchar(500) COLLATE utf8mb4_unicode_ci NULL AFTER `description`;
