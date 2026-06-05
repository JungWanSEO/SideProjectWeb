-- V2: OAuth2/SSO 대비 — member 테이블 로그인 설계 확장.
--  · 소셜 로그인 유저는 로컬 비밀번호가 없음 → password nullable.
--  · 어디서 인증했는지(provider)와 그 제공자의 고유 ID(provider_id) 보관.
-- (소셜 로그인 자체 구현은 후속. 지금은 모델만 선제 준비 — 로컬 로그인엔 영향 없음.)
-- provider enum 값은 알파벳순(AuthProvider 및 Hibernate ENUM DDL과 일치).

ALTER TABLE `member`
  MODIFY COLUMN `password` varchar(255) COLLATE utf8mb4_unicode_ci NULL;

ALTER TABLE `member`
  ADD COLUMN `provider` enum('GOOGLE','KAKAO','LOCAL','NAVER')
      COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'LOCAL';

ALTER TABLE `member`
  ADD COLUMN `provider_id` varchar(255) COLLATE utf8mb4_unicode_ci NULL;
