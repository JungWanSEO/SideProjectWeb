package com.commerce.api.payment.gateway;

import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.payment.gateway.PaymentGateway.PaymentApprovalCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayRouter.class);

    private final Map<String, PaymentGateway> byProvider;
    private final String defaultProvider;
    private final Set<String> unavailableProviders;   // 설정상 down 처리할 PG(점검/서킷) — 페일오버 대상

    public PaymentGatewayRouter(List<PaymentGateway> gateways,
                                @Value("${payment.default-provider:TOSS}") String defaultProvider,
                                @Value("${payment.unavailable-providers:}") String unavailableProviders) {
        this.byProvider = gateways.stream()
                .collect(Collectors.toMap(g -> g.provider().toUpperCase(), Function.identity()));
        this.defaultProvider = defaultProvider.toUpperCase();
        this.unavailableProviders = parseCsvUpper(unavailableProviders);
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

    /**
     * 페일오버 결제 승인 — 요청한 PG로 먼저 시도하고, 그 PG가 <b>장애(설정상 down)거나 승인 거절</b>이면
     * 다른 PG로 자동 대체한다(MPG-stretch). 첫 성공을 반환하고, 전부 실패하면 실패 결과를 돌려준다.
     *
     * <p>이게 라우터를 "전략의 자리"로 둔 이유다(MPG-1) — 결제 서비스는 이 메서드만 부르고, 페일오버 정책은
     * 여기 한 곳에 갇힌다. 실제 승인한 PG가 요청과 다를 수 있어 결과({@link PaymentRoutingResult})에 provider를
     * 담아 돌려준다(결제 기록·환불 라우팅이 실제 PG를 따라가야 하므로).
     *
     * <p>단순화: 실무는 <b>기술적 실패(타임아웃·5xx)에만 페일오버</b>하고 한도초과 같은 <b>비즈니스 거절은
     * 재시도하지 않는다</b>(다른 PG도 거절). 모의 단계라 승인 실패면 일단 다음 PG로 넘긴다.
     *
     * @param requestedProvider 클라이언트가 고른 PG(null/blank면 기본 PG). 미지원 PG면 {@link #resolve}가 400.
     */
    public PaymentRoutingResult approveWithFailover(String requestedProvider, PaymentApprovalCommand command) {
        String primary = resolve(requestedProvider).provider().toUpperCase();   // 검증(미지원 400) + 정규화
        List<String> attempted = new ArrayList<>();
        String lastReason = "사용 가능한 PG 없음";

        for (String provider : failoverOrder(primary)) {
            attempted.add(provider);
            if (unavailableProviders.contains(provider)) {
                lastReason = "PG 사용 불가(설정상 down): " + provider;
                log.warn("결제 라우팅: {} 사용 불가 → 다음 PG로 페일오버", provider);
                continue;   // 장애 PG는 호출 안 하고 건너뜀
            }
            PaymentApproval approval = byProvider.get(provider).approve(command);
            if (approval.approved()) {
                if (attempted.size() > 1) {
                    log.warn("결제 라우팅: {} 실패 → {}로 페일오버 승인 성공 (시도순서 {})", primary, provider, attempted);
                }
                return new PaymentRoutingResult(provider, approval, List.copyOf(attempted));
            }
            lastReason = approval.failureReason();
            log.warn("결제 라우팅: {} 승인 거절({}) → 다음 PG로 페일오버", provider, lastReason);
        }

        String last = attempted.isEmpty() ? primary : attempted.get(attempted.size() - 1);
        return new PaymentRoutingResult(last,
                PaymentApproval.failed("모든 PG 결제 실패: " + lastReason), List.copyOf(attempted));
    }

    /** 페일오버 시도 순서 — 요청 PG 먼저, 그다음 나머지 PG를 알파벳순(결정적). */
    private List<String> failoverOrder(String primary) {
        List<String> order = new ArrayList<>();
        order.add(primary);
        byProvider.keySet().stream().filter(p -> !p.equals(primary)).sorted().forEach(order::add);
        return order;
    }

    private static Set<String> parseCsvUpper(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toUpperCase)
                .collect(Collectors.toUnmodifiableSet());
    }
}
