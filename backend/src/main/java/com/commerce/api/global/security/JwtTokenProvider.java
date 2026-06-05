package com.commerce.api.global.security;

import com.commerce.api.member.entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT 생성/검증 담당.
 * - subject = 회원 ID, claim "role" = 권한(액세스 토큰만).
 * - HS256 서명 (secret 기반).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    /** 액세스 토큰: 회원 ID + 권한 */
    public String createAccessToken(Long memberId, Role role) {
        return buildToken(memberId, role.name(), accessTokenValidityMs);
    }

    /** 리프레시 토큰: 회원 ID만 (권한 없음) */
    public String createRefreshToken(Long memberId) {
        return buildToken(memberId, null, refreshTokenValidityMs);
    }

    private String buildToken(Long memberId, String role, long validityMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + validityMs);
        var builder = Jwts.builder()
                .id(UUID.randomUUID().toString())   // jti: 매 발급마다 고유 → 같은 초에 만들어도 토큰이 달라짐(회전·재사용탐지)
                .subject(String.valueOf(memberId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key);
        if (role != null) {
            builder.claim("role", role);
        }
        return builder.compact();
    }

    public boolean validate(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long getMemberId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }

    public String getRole(String token) {
        return parse(token).get("role", String.class);
    }

    private Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
