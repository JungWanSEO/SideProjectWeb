package com.commerce.api.payment.gateway;

import com.commerce.api.global.exception.BusinessException;
import com.commerce.api.payment.gateway.PaymentGateway.PaymentApprovalCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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
 * <p>이 라우터가 <b>라우팅 전략의 자리</b>다 — 결제 서비스 코드를 안 건드리고 정책을 여기 모은다:
 * ① 클라이언트 선택(provider) ② <b>페일오버</b>(장애·거절 시 다른 PG로, MPG-stretch)
 * ③ <b>비용기반</b>({@link #AUTO}면 최저 요율 PG, 페일오버도 비용 오름차순).
 */
@Component
public class PaymentGatewayRouter {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayRouter.class);

    /** 클라이언트가 PG를 직접 안 고르고 "가장 싼 PG로 알아서" 할 때 쓰는 값(비용기반 라우팅). */
    public static final String AUTO = "AUTO";

    /** 요율을 모르는 PG의 폴백 요율 — 보수적으로 가장 높게(3.0%). (게이트웨이가 없는 provider 방어) */
    public static final double DEFAULT_FEE_RATE = 0.030;

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
     * 해당 PG의 수수료율. 요율은 PG 고유 속성이라 게이트웨이가 단일 출처(MPG 비용기반).
     * 정산(settlement)이 수수료를 계산할 때 이 메서드로 읽는다(settlement → payment 정방향).
     * provider가 null/blank거나 등록되지 않은 PG면 {@link #DEFAULT_FEE_RATE}.
     */
    public double feeRateOf(String provider) {
        if (provider == null || provider.isBlank()) {
            return DEFAULT_FEE_RATE;
        }
        PaymentGateway gateway = byProvider.get(provider.toUpperCase());
        return gateway != null ? gateway.feeRate() : DEFAULT_FEE_RATE;
    }

    /** 비용기반 라우팅 — 등록된 PG 중 수수료율이 가장 낮은 PG(동률이면 알파벳순). */
    private String cheapestProvider() {
        return byProvider.keySet().stream()
                .min(Comparator.comparingDouble((String p) -> byProvider.get(p).feeRate())
                        .thenComparing(Comparator.naturalOrder()))
                .orElseThrow(() -> new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "등록된 결제 PG가 없습니다."));
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
     * <p><b>비용기반:</b> {@code requestedProvider}가 {@link #AUTO}면 가장 싼 PG를 1차로 고른다. 페일오버
     * 순서도 비용 오름차순(싼 PG부터)이라, AUTO든 명시든 더 싼 대체 PG로 먼저 넘어간다.
     *
     * @param requestedProvider 클라이언트가 고른 PG. null/blank면 기본 PG, {@link #AUTO}면 최저가 PG. 미지원 PG면 400.
     */
    public PaymentRoutingResult approveWithFailover(String requestedProvider, PaymentApprovalCommand command) {
        // AUTO면 최저가 PG, 아니면 요청 PG(미지원이면 resolve가 400) — 둘 다 대문자 정규화
        String primary = AUTO.equalsIgnoreCase(requestedProvider)
                ? cheapestProvider()
                : resolve(requestedProvider).provider().toUpperCase();
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

    /** 페일오버 시도 순서 — 요청(또는 최저가) PG 먼저, 그다음 나머지를 <b>비용 오름차순</b>(동률이면 알파벳순). */
    private List<String> failoverOrder(String primary) {
        List<String> order = new ArrayList<>();
        order.add(primary);
        byProvider.keySet().stream()
                .filter(p -> !p.equals(primary))
                .sorted(Comparator.comparingDouble((String p) -> byProvider.get(p).feeRate())
                        .thenComparing(Comparator.naturalOrder()))
                .forEach(order::add);
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
