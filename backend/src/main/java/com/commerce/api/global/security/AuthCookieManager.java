package com.commerce.api.global.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * JWT를 담는 httpOnly 쿠키를 만들고/지운다.
 *
 * <p>토큰 보관 전략: access·refresh 모두 <b>httpOnly 쿠키</b>에 둔다.
 * <ul>
 *   <li><b>httpOnly</b> → JS(document.cookie)에서 읽을 수 없음 → XSS로 토큰 탈취 방지.</li>
 *   <li><b>SameSite=Lax</b> → 다른 사이트發 요청엔 쿠키를 안 실어줌 → CSRF 차단.</li>
 *   <li><b>secure</b> → https에서만 전송. dev(http)는 false, 운영(https)은 true로.</li>
 * </ul>
 */
@Component
public class AuthCookieManager {

    /** @CookieValue 등에서 참조할 수 있도록 public 상수. */
    public static final String ACCESS_COOKIE = "access_token";
    public static final String REFRESH_COOKIE = "refresh_token";

    // TODO(운영): https 배포 시 SECURE=true. 프론트/API 도메인이 다르면 SameSite=None + CSRF 토큰 검토.
    private static final boolean SECURE = false;     // dev(http)
    private static final String SAME_SITE = "Lax";   // CSRF 차단

    private final long accessMaxAgeSec;
    private final long refreshMaxAgeSec;

    public AuthCookieManager(
            @Value("${jwt.access-token-validity-ms}") long accessValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshValidityMs) {
        this.accessMaxAgeSec = accessValidityMs / 1000;
        this.refreshMaxAgeSec = refreshValidityMs / 1000;
    }

    public ResponseCookie accessCookie(String token) {
        return build(ACCESS_COOKIE, token, accessMaxAgeSec);
    }

    public ResponseCookie refreshCookie(String token) {
        return build(REFRESH_COOKIE, token, refreshMaxAgeSec);
    }

    /** 로그아웃/만료용: 값 비우고 maxAge=0 → 브라우저가 즉시 삭제. */
    public ResponseCookie clearAccessCookie() {
        return build(ACCESS_COOKIE, "", 0);
    }

    public ResponseCookie clearRefreshCookie() {
        return build(REFRESH_COOKIE, "", 0);
    }

    private ResponseCookie build(String name, String value, long maxAgeSec) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(SECURE)
                .sameSite(SAME_SITE)
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSec))
                .build();
    }
}
