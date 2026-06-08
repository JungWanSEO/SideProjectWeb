package com.commerce.api.payment.gateway;

import com.commerce.api.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 결제 PG 라우터(레지스트리) — 여러 {@link PaymentGateway} 어댑터 중 알맞은 것을 골라준다.
 *
 * <p>스프링이 모든 {@code PaymentGateway} 빈을 주입 → {@link PaymentGateway#provider()}로 맵핑한다.
 * {@code InProcessEventPublisher}가 핸들러를 {@code eventType}별로 모으는 것과 같은 패턴(포트-어댑터 + 레지스트리).
 *
 * <p>지금은 클라이언트가 고른 provider를 그대로 위임하지만, 이 라우터가 <b>라우팅 전략의 자리</b>다 —
 * 추후 "기본 PG 장애 시 다른 PG로 페일오버", "비용 기반 선택" 같은 정책을 여기에 넣으면 결제 서비스 코드는
 * 바뀌지 않는다(전략을 한 곳에 가둠).
 */
@Component
public class PaymentGatewayRouter {

    private final Map<String, PaymentGateway> byProvider;
    private final String defaultProvider;

    public PaymentGatewayRouter(List<PaymentGateway> gateways,
                                @Value("${payment.default-provider:TOSS}") String defaultProvider) {
        this.byProvider = gateways.stream()
                .collect(Collectors.toMap(g -> g.provider().toUpperCase(), Function.identity()));
        this.defaultProvider = defaultProvider.toUpperCase();
    }

    /**
     * provider로 어댑터를 선택한다. null/blank면 기본 PG를 쓰고, 지원하지 않는 PG면 400.
     * (결제 요청 시 선택, 환불 시 결제에 저장된 provider로 같은 PG에 라우팅하는 데 함께 쓴다.)
     */
    public PaymentGateway resolve(String provider) {
        String key = (provider == null || provider.isBlank()) ? defaultProvider : provider.toUpperCase();
        PaymentGateway gateway = byProvider.get(key);
        if (gateway == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "지원하지 않는 결제 PG입니다: " + provider + " (지원: " + byProvider.keySet() + ")");
        }
        return gateway;
    }

    /**
     * 모든 PG의 정산 리포트를 합쳐 돌려준다 — 대사(reconciliation)가 여러 PG를 한 번에 대조한다.
     * 거래 ID 프리픽스가 PG를 구분하므로 키가 겹치지 않는다.
     */
    public List<PgSettlementRecord> fetchAllSettlements() {
        return byProvider.values().stream()
                .flatMap(gateway -> gateway.fetchSettlements().stream())
                .toList();
    }
}
