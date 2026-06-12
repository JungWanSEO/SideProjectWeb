package com.commerce.api.address.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerce.api.address.dto.AddressResponse;
import com.commerce.api.address.service.AddressService;
import java.time.LocalDateTime;
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
 * AddressController 통합 테스트 (@WebMvcTest + MockMvc).
 * 보안 필터 비활성 + 로그인 회원(principal=1L) 주입.
 */
@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AddressService addressService;

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

    private List<AddressResponse> sampleList() {
        return List.of(new AddressResponse(
                10L, "홍길동", "010-1234-5678", "06236", "서울 강남구 테헤란로 123", "4층", true, LocalDateTime.now()));
    }

    @Test
    @DisplayName("GET /api/addresses - 목록 조회 200")
    void getMyAddresses_success() throws Exception {
        given(addressService.getMyAddresses(1L)).willReturn(sampleList());

        mockMvc.perform(get("/api/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].recipient").value("홍길동"))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    @Test
    @DisplayName("POST /api/addresses - 추가 200")
    void create_success() throws Exception {
        given(addressService.create(eq(1L), any())).willReturn(sampleList());

        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"홍길동","phone":"010-1234-5678","zipcode":"06236","address1":"서울 강남구 테헤란로 123","address2":"4층"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].zipcode").value("06236"));
    }

    @Test
    @DisplayName("POST /api/addresses - 필수값(수령인) 누락 시 400")
    void create_validationFail() throws Exception {
        mockMvc.perform(post("/api/addresses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"","phone":"010-1234-5678","zipcode":"06236","address1":"서울"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("PUT /api/addresses/{id} - 수정 200")
    void update_success() throws Exception {
        given(addressService.update(eq(1L), eq(10L), any())).willReturn(sampleList());

        mockMvc.perform(put("/api/addresses/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recipient":"김철수","phone":"010-9999-8888","zipcode":"12345","address1":"부산 해운대구"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PUT /api/addresses/{id}/default - 기본배송지 지정 200")
    void setDefault_success() throws Exception {
        given(addressService.setDefault(1L, 10L)).willReturn(sampleList());

        mockMvc.perform(put("/api/addresses/10/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    @Test
    @DisplayName("DELETE /api/addresses/{id} - 삭제 200")
    void delete_success() throws Exception {
        given(addressService.delete(anyLong(), anyLong())).willReturn(List.of());

        mockMvc.perform(delete("/api/addresses/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
