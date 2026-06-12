-- V16: 배송지(주소록) 도메인 — Phase 1 #4 Step 2.
--  · address 테이블: 회원의 저장된 배송지. 회원당 기본배송지(is_default) 1개 불변식은 앱(AddressService)이 보장.
--  · 회원은 다른 애그리거트라 FK 제약은 두지 않는다(ID 참조 원칙 — architecture.md §11). 조회용 인덱스만.
--  · is_default: Hibernate(MySQL)는 boolean을 bit로 매핑 → bit 컬럼(validate 일치).

CREATE TABLE `address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `updated_at` datetime(6) DEFAULT NULL,
  `member_id` bigint NOT NULL,
  `recipient` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `zipcode` varchar(10) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address1` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL,
  `address2` varchar(200) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `is_default` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_address_member` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
