-- V15: 리뷰·평점 도메인 — Phase 1 #2.
--  · review 테이블: 1인 1상품 1리뷰(member_id+product_id UNIQUE), rating 1~5, content, 사진리뷰 image_url(선택).
--  · product에 평점 비정규화 카운터(rating_count, rating_sum) 추가 — 리뷰 작성/삭제 시 원자 UPDATE로 증감.
--    평균 = rating_sum / rating_count (앱에서 계산). 기존 상품 행은 DEFAULT 0.
--  · 회원·상품은 다른 애그리거트라 FK 제약은 두지 않는다(ID 참조 원칙 — architecture.md §11).

ALTER TABLE `product`
  ADD COLUMN `rating_count` int NOT NULL DEFAULT 0 AFTER `image_url`,
  ADD COLUMN `rating_sum` int NOT NULL DEFAULT 0 AFTER `rating_count`;

CREATE TABLE `review` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `rating` int NOT NULL,
  `content` varchar(1000) COLLATE utf8mb4_unicode_ci NOT NULL,
  `image_url` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_member_product` (`member_id`, `product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
