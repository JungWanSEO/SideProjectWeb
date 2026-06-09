package com.commerce.api.payment.gateway;

import java.util.List;

/**
 * 페일오버 라우팅 결과 — 어느 PG가 (최종) 처리했는지 + 승인 결과 + 시도한 PG 순서.
 *
 * <p>요청한 PG가 장애·거절이면 라우터가 다른 PG로 넘기므로, <b>실제 승인한 provider가 요청과 다를 수 있다.</b>
 * 그래서 결과에 provider를 담아 Payment에 "실제 처리 PG"를 기록한다(환불도 이 PG로 라우팅해야 하므로).
 *
 * @param provider  실제 승인한 PG(성공 시) 또는 마지막으로 시도한 PG(전부 실패 시)
 * @param approval  PG 승인 결과(성공/실패)
 * @param attempted 시도한 provider 순서(관측·디버깅용 — 예: [KAKAOPAY, TOSS])
 */
public record PaymentRoutingResult(String provider, PaymentApproval approval, List<String> attempted) {
}
