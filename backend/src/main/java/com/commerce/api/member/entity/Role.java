package com.commerce.api.member.entity;

/**
 * 회원 권한.
 * Spring Security 권한 문자열은 "ROLE_" 접두사 규칙을 따른다 → ROLE_USER / ROLE_ADMIN.
 */
public enum Role {
    USER,
    ADMIN
}
