package com.commerce.api.payment.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.payment.gateway.PaymentGateway.PaymentApprovalCommand;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * PaymentGatewayRouter 단위 테스트 — provider로 어댑터 선택 + 다중 PG 정산 집계.
 * (실제 모의 어댑터를 그대로 쓴다 — 의존성이 없어 목이 불필요.)
 */
class PaymentGatewayRouterTest {

    private TossMockGateway toss;
    private KakaoPayMockGateway kakao;
    private PaymentGatewayRouter router;

    @BeforeEach
    void setUp() {
        toss = new TossMockGateway();
        kakao = new KakaoPayMockGateway();
        router = new PaymentGatewayRouter(List.of(toss, kakao), "TOSS");
    }

    @Test
    @DisplayName("provider로 어댑터 선택 - TOSS / KAKAOPAY")
    void resolve_byProvider() {
        assertThat(router.resolve("TOSS")).isSameAs(toss);
        assertThat(router.resolve("KAKAOPAY")).isSameAs(kakao);
    }

    @Test
    @DisplayName("대소문자 무시 - 소문자 provider도 매칭")
    void resolve_caseInsensitive() {
        assertThat(router.resolve("kakaopay")).isSameAs(kakao);
    }

    @Test
    @DisplayName("미지정(null/blank) - 기본 PG로 폴백")
    void resolve_defaultsWhenBlank() {
        assertThat(router.resolve(null)).isSameAs(toss);
        assertThat(router.resolve("  ")).isSameAs(toss);
    }

    @Test
    @DisplayName("지원하지 않는 PG - 400")
    void resolve_unsupported() {
        assertThatThrownBy(() -> router.resolve("PAYPAL"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 결제 PG");
    }

    @Test
    @DisplayName("정산 집계 - 모든 PG 원장을 합쳐 돌려주고, 거래 ID 프리픽스로 PG가 구분된다")
    void fetchAllSettlements_aggregatesAcrossPgs() {
        String tossTx = toss.approve(new PaymentApprovalCommand(1L, 10000L, "k1")).pgTransactionId();
        String kakaoTx = kakao.approve(new PaymentApprovalCommand(2L, 20000L, "k2")).pgTransactionId();

        List<PgSettlementRecord> all = router.fetchAllSettlements();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(PgSettlementRecord::pgTransactionId)
                .containsExactlyInAnyOrder(tossTx, kakaoTx);
        assertThat(tossTx).startsWith("TOSS-");
        assertThat(kakaoTx).startsWith("KAKAO-");
    }
}
