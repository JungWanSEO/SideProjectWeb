package com.commerce.api.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.payment.dto.PaymentResponse;
import com.commerce.api.payment.entity.PaymentStatus;
import com.commerce.api.payment.service.PaymentService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * PaymentController 통합 테스트 (@WebMvcTest + MockMvc).
 * 보안 필터는 비활성하고, 현재 로그인 회원(principal=memberId)을 SecurityContext에 직접 주입한다.
 */
@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @BeforeEach
    void setAuth() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        1L, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private PaymentResponse sample(PaymentStatus status) {
        return new PaymentResponse(1L, 1L, 30000L, status, "MOCK_CARD", "TOSS", "MOCK-tx-1", LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/payments - 결제 성공 시 201, 상태 PAID")
    void pay_success() throws Exception {
        given(paymentService.pay(eq(1L), any())).willReturn(sample(PaymentStatus.PAID));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":1,"idempotencyKey":"key-1","method":"MOCK_CARD"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.amount").value(30000));
    }

    @Test
    @DisplayName("POST /api/payments - orderId 누락이면 400")
    void pay_validationFail() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idempotencyKey":"key-1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/payments - PG 거절 시 402")
    void pay_declined() throws Exception {
        given(paymentService.pay(eq(1L), any()))
                .willThrow(new BusinessException(HttpStatus.PAYMENT_REQUIRED, "결제가 거절되었습니다."));

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":1,"idempotencyKey":"key-1"}
                                """))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.success").value(false));
    }
}
