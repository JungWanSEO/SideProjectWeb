package com.commerce.api.global.security;

import com.commerce.api.global.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 미인증 상태(토큰 없음/만료/무효)로 보호 자원에 접근할 때 → <b>401</b> JSON 응답.
 * (기본은 httpBasic/formLogin off라 403이 떨어졌음 → 401로 교정해 "권한 부족(403)"과 구분.)
 * 컨트롤러 밖(필터 단계)이라 GlobalExceptionHandler를 못 타므로 여기서 직접 JSON을 쓴다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error("인증이 필요합니다."));
    }
}
