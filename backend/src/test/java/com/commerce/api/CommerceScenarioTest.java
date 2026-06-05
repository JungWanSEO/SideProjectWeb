package com.commerce.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.commerce.api.member.entity.Member;
import com.commerce.api.member.entity.Role;
import com.commerce.api.member.repository.MemberRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 전체 구매 흐름 통합 테스트 (@SpringBootTest + MockMvc, 보안 필터 ON, 실제 JWT 사용).
 * (ADMIN)상품등록 → (USER)가입·로그인 → 주문 → 결제(PAID) → 재고차감 → 취소 → 재고복원 → 장바구니 → 체크아웃 → 결제.
 *
 * <p>인증은 <b>httpOnly 쿠키</b> 기반: 로그인 응답의 Set-Cookie(access/refresh)를 받아 이후 요청에 그대로 재전송한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommerceScenarioTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /** 로그인 후 응답에 실린 인증 쿠키(access_token·refresh_token)를 반환 → 이후 요청에 .cookie(...)로 재전송. */
    private Cookie[] loginAndGetCookies(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getCookies();
    }

    private long dataId(String responseJson) throws Exception {
        return objectMapper.readTree(responseJson).path("data").path("id").asLong();
    }

    /** 상품 응답에서 첫 옵션의 id를 꺼낸다. */
    private long firstOptionId(String responseJson) throws Exception {
        return objectMapper.readTree(responseJson).path("data").path("options").get(0).path("id").asLong();
    }

    @Test
    @DisplayName("전체 구매 흐름: ADMIN 상품등록 → USER 가입·로그인 → 주문 → 결제 → 취소(재고복원) → 장바구니 → 체크아웃 → 결제")
    void fullPurchaseFlow() throws Exception {
        // 0) ADMIN 시드 + 로그인 (쿠키 획득)
        memberRepository.save(Member.builder()
                .email("admin@commerce.com")
                .password(passwordEncoder.encode("password123"))
                .nickname("admin")
                .role(Role.ADMIN)
                .build());
        Cookie[] adminCookies = loginAndGetCookies("admin@commerce.com", "password123");

        // 1) ADMIN 상품 등록 (재고 10)
        String productJson = mockMvc.perform(post("/api/products")
                        .cookie(adminCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"sneakers\",\"price\":50000,\"description\":\"limited\","
                                + "\"options\":[{\"size\":\"270\",\"stock\":10}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ON_SALE"))
                .andReturn().getResponse().getContentAsString();
        long productId = dataId(productJson);
        long optionId = firstOptionId(productJson);   // 주문은 옵션(사이즈) 단위

        // 2) USER 가입
        mockMvc.perform(post("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@commerce.com\",\"password\":\"password123\",\"nickname\":\"user\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("USER"));

        // 3) USER 로그인 (쿠키 획득)
        Cookie[] userCookies = loginAndGetCookies("user@commerce.com", "password123");

        // 4) 인증 없이 주문 → 4xx (보안 동작 확인)
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"optionId\":" + optionId + ",\"quantity\":3}]}"))
                .andExpect(status().is4xxClientError());

        // 5) USER 주문 (3개) → 201, 총액 150000, 스냅샷 상품명
        String orderJson = mockMvc.perform(post("/api/orders")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"optionId\":" + optionId + ",\"quantity\":3}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalPrice").value(150000))
                .andExpect(jsonPath("$.data.items[0].productName").value("sneakers"))
                .andExpect(jsonPath("$.data.items[0].size").value("270"))
                .andReturn().getResponse().getContentAsString();
        long orderId = dataId(orderJson);

        // 6) 결제(모의 PG 승인) → 201, PAID
        mockMvc.perform(post("/api/payments")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + orderId + ",\"idempotencyKey\":\"idem-order-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // 7) 결제 후 옵션 재고 차감(10 → 7) + 주문 PAID 확인
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options[0].stock").value(7));
        mockMvc.perform(get("/api/orders/" + orderId)
                        .cookie(userCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // 8) 주문 취소 → CANCELLED
        mockMvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .cookie(userCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        // 9) 결제된 주문을 취소했으므로 옵션 재고가 복원된다 (7 → 10)
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options[0].stock").value(10));

        // 10) 장바구니 담기(옵션 단위) + 조회 (현재 상품/옵션 정보로 enrich)
        mockMvc.perform(post("/api/carts/items")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"optionId\":" + optionId + ",\"quantity\":2}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/carts")
                        .cookie(userCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(2))
                .andExpect(jsonPath("$.data.items[0].productName").value("sneakers"))
                .andExpect(jsonPath("$.data.items[0].optionId").value((int) optionId));

        // 11) 체크아웃: 장바구니 → PENDING 주문 + 장바구니 비우기 (한 트랜잭션)
        String checkoutJson = mockMvc.perform(post("/api/orders/checkout")
                        .cookie(userCookies))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.totalPrice").value(100000))   // 50000 x 2
                .andExpect(jsonPath("$.data.items[0].productName").value("sneakers"))
                .andReturn().getResponse().getContentAsString();
        long checkoutOrderId = dataId(checkoutJson);

        // 11-2) 체크아웃 주문 결제 → PAID
        mockMvc.perform(post("/api/payments")
                        .cookie(userCookies)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":" + checkoutOrderId + ",\"idempotencyKey\":\"idem-checkout-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PAID"));

        // 12) 체크아웃+결제 후: 장바구니 비워짐 + 재고 차감(10 → 8)
        mockMvc.perform(get("/api/carts")
                        .cookie(userCookies))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalQuantity").value(0));
        mockMvc.perform(get("/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.options[0].stock").value(8));
    }
}
