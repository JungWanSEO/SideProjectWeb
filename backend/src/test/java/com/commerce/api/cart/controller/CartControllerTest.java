package com.commerce.api.cart.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerce.api.cart.dto.CartResponse;
import com.commerce.api.cart.dto.CartResponse.CartItemResponse;
import com.commerce.api.cart.service.CartService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CartController 통합 테스트 (@WebMvcTest + MockMvc).
 * 보안 필터 비활성 + 로그인 회원(principal=1L)을 SecurityContext에 주입.
 */
@WebMvcTest(CartController.class)
@AutoConfigureMockMvc(addFilters = false)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

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

    private CartResponse sampleCart() {
        // CartItemResponse(productId, optionId, productName, size, price, quantity, subtotal, stock, soldOut)
        return new CartResponse(1L,
                List.of(new CartItemResponse(1L, 10L, "반팔티셔츠", "M", 10000L, 2, 20000L, 50, false)), 2);
    }

    @Test
    @DisplayName("POST /api/carts/items - 담기 성공 시 200")
    void addItem_success() throws Exception {
        given(cartService.addItem(eq(1L), any())).willReturn(sampleCart());

        mockMvc.perform(post("/api/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"optionId":10,"quantity":2}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.items[0].optionId").value(10))
                .andExpect(jsonPath("$.data.items[0].size").value("M"))
                .andExpect(jsonPath("$.data.items[0].subtotal").value(20000));
    }

    @Test
    @DisplayName("POST /api/carts/items - 수량이 0 이하면 400")
    void addItem_validationFail() throws Exception {
        mockMvc.perform(post("/api/carts/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"optionId":10,"quantity":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/carts - 조회 성공 시 200")
    void getCart_success() throws Exception {
        given(cartService.getCart(1L)).willReturn(sampleCart());

        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].productName").value("반팔티셔츠"))
                .andExpect(jsonPath("$.data.items[0].size").value("M"));
    }

    @Test
    @DisplayName("DELETE /api/carts/items/{optionId} - 제거 성공 시 200")
    void removeItem_success() throws Exception {
        given(cartService.removeItem(anyLong(), anyLong()))
                .willReturn(new CartResponse(1L, List.of(), 0));

        mockMvc.perform(delete("/api/carts/items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(0));
    }
}
