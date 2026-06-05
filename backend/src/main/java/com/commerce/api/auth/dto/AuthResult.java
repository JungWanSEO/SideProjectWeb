package com.commerce.api.auth.dto;

import com.commerce.api.member.dto.MemberResponse;

/**
 * 인증 결과 (서비스 → 컨트롤러 내부 전달용).
 * 토큰 문자열은 컨트롤러가 httpOnly 쿠키로 굽고, user 정보만 응답 body로 내보낸다.
 * (클라이언트는 토큰을 직접 받지 않는다 — httpOnly 전략)
 */
public record AuthResult(
        String accessToken,
        String refreshToken,
        MemberResponse user
) {
}
