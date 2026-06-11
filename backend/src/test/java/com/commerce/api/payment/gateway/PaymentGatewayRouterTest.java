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
        router = new PaymentGatewayRouter(List.of(toss, kakao), "TOSS", "");   // 미사용 PG 없음
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

    // ---------- 페일오버 (MPG-stretch) ----------

    @Test
    @DisplayName("페일오버 없음 - 요청 PG가 정상이면 그대로 승인(시도 1회)")
    void failover_noneNeeded() {
        PaymentRoutingResult r = router.approveWithFailover("TOSS", new PaymentApprovalCommand(1L, 10000L, "k1"));

        assertThat(r.approval().approved()).isTrue();
        assertThat(r.provider()).isEqualTo("TOSS");
        assertThat(r.attempted()).containsExactly("TOSS");
        assertThat(r.approval().pgTransactionId()).startsWith("TOSS-");
    }

    @Test
    @DisplayName("페일오버 - 요청 PG가 설정상 down이면 건너뛰고 다른 PG로 승인")
    void failover_onUnavailableProvider() {
        PaymentGatewayRouter r = new PaymentGatewayRouter(List.of(toss, kakao), "TOSS", "KAKAOPAY");

        PaymentRoutingResult result = r.approveWithFailover("KAKAOPAY", new PaymentApprovalCommand(1L, 10000L, "k1"));

        assertThat(result.approval().approved()).isTrue();
        assertThat(result.provider()).isEqualTo("TOSS");                 // 카카오 down → 토스로 대체
        assertThat(result.attempted()).containsExactly("KAKAOPAY", "TOSS");
        assertThat(result.approval().pgTransactionId()).startsWith("TOSS-");
    }

    @Test
    @DisplayName("페일오버 - 요청 PG 승인이 거절되면 다음 PG로 승인")
    void failover_onApprovalDeclined() {
        FailingGateway failingToss = new FailingGateway();
        PaymentGatewayRouter r = new PaymentGatewayRouter(List.of(failingToss, kakao), "TOSS", "");

        PaymentRoutingResult result = r.approveWithFailover("TOSS", new PaymentApprovalCommand(1L, 10000L, "k1"));

        assertThat(result.approval().approved()).isTrue();
        assertThat(result.provider()).isEqualTo("KAKAOPAY");             // 토스 거절 → 카카오로 대체
        assertThat(result.attempted()).containsExactly("TOSS", "KAKAOPAY");
    }

    @Test
    @DisplayName("페일오버 - 모든 PG가 down이면 승인 실패 결과(시도 순서는 보존)")
    void failover_allUnavailable() {
        PaymentGatewayRouter r = new PaymentGatewayRouter(List.of(toss, kakao), "TOSS", "TOSS,KAKAOPAY");

        PaymentRoutingResult result = r.approveWithFailover("TOSS", new PaymentApprovalCommand(1L, 10000L, "k1"));

        assertThat(result.approval().approved()).isFalse();
        assertThat(result.approval().failureReason()).contains("모든 PG 결제 실패");
        assertThat(result.attempted()).containsExactlyInAnyOrder("TOSS", "KAKAOPAY");
    }

    @Test
    @DisplayName("페일오버 - 미지원 PG 요청이면 폴백 전에 400(검증 먼저)")
    void failover_unsupportedRequested() {
        assertThatThrownBy(() -> router.approveWithFailover("PAYPAL", new PaymentApprovalCommand(1L, 10000L, "k1")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("지원하지 않는 결제 PG");
    }

    // ---------- 비용기반 라우팅 ----------

    @Test
    @DisplayName("feeRateOf - 게이트웨이 요율 반환, 미등록/blank는 폴백(3.0%)")
    void feeRateOf_returnsGatewayRate() {
        assertThat(router.feeRateOf("TOSS")).isEqualTo(0.025);
        assertThat(router.feeRateOf("kakaopay")).isEqualTo(0.028);   // 대소문자 무시
        assertThat(router.feeRateOf("PAYPAL")).isEqualTo(PaymentGatewayRouter.DEFAULT_FEE_RATE);
        assertThat(router.feeRateOf(null)).isEqualTo(PaymentGatewayRouter.DEFAULT_FEE_RATE);
    }

    @Test
    @DisplayName("AUTO - 가장 싼 PG로 라우팅(요율 최소값 선택, 기본 PG 하드코딩 아님)")
    void approveWithFailover_auto_picksCheapest() {
        // toss 2.5% / kakao 2.8% → 둘만 있으면 TOSS가 최저
        PaymentRoutingResult r = router.approveWithFailover("AUTO", new PaymentApprovalCommand(1L, 10000L, "k1"));
        assertThat(r.approval().approved()).isTrue();
        assertThat(r.provider()).isEqualTo("TOSS");
        assertThat(r.attempted().get(0)).isEqualTo("TOSS");

        // 더 싼 PG(2.0%)를 추가하면 AUTO는 그쪽으로 — 최소값을 실제로 고른다는 증거
        PaymentGatewayRouter r2 = new PaymentGatewayRouter(List.of(toss, kakao, new CheapGateway()), "TOSS", "");
        PaymentRoutingResult result = r2.approveWithFailover("AUTO", new PaymentApprovalCommand(1L, 10000L, "k1"));
        assertThat(result.provider()).isEqualTo("CHEAPPAY");
    }

    @Test
    @DisplayName("페일오버 - 비용 오름차순: 1차가 down이면 다음으로 싼 PG로 넘어간다")
    void approveWithFailover_costOrdered() {
        // CHEAPPAY(2.0%) down → AUTO는 1차=CHEAPPAY지만 사용 불가 → 다음으로 싼 TOSS(2.5%)로(카카오 2.8% 아님)
        PaymentGatewayRouter r = new PaymentGatewayRouter(List.of(toss, kakao, new CheapGateway()), "TOSS", "CHEAPPAY");
        PaymentRoutingResult result = r.approveWithFailover("AUTO", new PaymentApprovalCommand(1L, 10000L, "k1"));
        assertThat(result.approval().approved()).isTrue();
        assertThat(result.provider()).isEqualTo("TOSS");                 // 다음으로 싼 PG
        assertThat(result.attempted()).containsExactly("CHEAPPAY", "TOSS");
    }

    /** 승인을 항상 거절하는 토스 모의(페일오버 검증용). 환불·원장은 베이스 그대로. */
    private static final class FailingGateway extends AbstractMockPaymentGateway {
        @Override
        public String provider() {
            return "TOSS";
        }

        @Override
        public double feeRate() {
            return 0.025;
        }

        @Override
        protected String idPrefix() {
            return "TOSS";
        }

        @Override
        public PaymentApproval approve(PaymentApprovalCommand command) {
            return PaymentApproval.failed("PG 점검중(모의)");
        }
    }

    /** 가장 싼 모의 PG(2.0%) — 비용기반 선택이 최소값을 실제로 고르는지 검증용. */
    private static final class CheapGateway extends AbstractMockPaymentGateway {
        @Override
        public String provider() {
            return "CHEAPPAY";
        }

        @Override
        public double feeRate() {
            return 0.020;
        }

        @Override
        protected String idPrefix() {
            return "CHEAP";
        }
    }
}
