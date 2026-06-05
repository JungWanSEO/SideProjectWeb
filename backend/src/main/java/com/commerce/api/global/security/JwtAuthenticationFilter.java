package com.commerce.api.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청 헤더의 'Authorization: Bearer <token>'를 검증하고,
 * 유효하면 SecurityContext에 인증 정보를 저장한다. (principal = 회원 ID)
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.validate(token)) {
            String role = jwtTokenProvider.getRole(token);
            if (role != null) {   // 권한이 있는 액세스 토큰만 인증으로 인정
                Long memberId = jwtTokenProvider.getMemberId(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        memberId, null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        // 1순위: Authorization 헤더 (Swagger·API 클라이언트·테스트)
        String bearer = request.getHeader(HEADER);
        if (bearer != null && bearer.startsWith(PREFIX)) {
            return bearer.substring(PREFIX.length());
        }
        // 2순위: httpOnly 쿠키의 access_token (브라우저 흐름)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (AuthCookieManager.ACCESS_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
