package com.commerce.api.member.entity;

/**
 * 인증 제공자. LOCAL = 자체 이메일/비밀번호, 나머지는 소셜 로그인(OAuth2/SSO).
 * (소셜 로그인 구현은 후속 — 지금은 유저 모델만 선제 대비.)
 *
 * <p>값 순서는 <b>알파벳순</b>으로 둔다 — Hibernate가 MySQL ENUM DDL을 알파벳순으로 생성/검증하므로
 * V1·V2 마이그레이션의 enum 정의와 일치시키기 위함.
 */
public enum AuthProvider {
    GOOGLE,
    KAKAO,
    LOCAL,
    NAVER
}
