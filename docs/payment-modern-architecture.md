# 결제 아키텍처: 옛날(동기 DLL) → 현대 (학습 노트)

> 목표: **실무에서 결제 아키텍처 의견을 낼 수 있게.** VAN사 DLL(KICC 등) 동기 연동을 겪은 경험을 좌표로,
> 현대 결제 시스템이 실제로 신경 쓰는 것을 우리 코드에 그려본다.
> 관련: 기초 → [architecture-basics.md](architecture-basics.md) · 패턴 적용 → [payment-architecture-study.md](payment-architecture-study.md)
> 작성 2026-06-07. 단계별로 누적(① 개요+웹훅 → ② 대사/정산 → …).

---

## 1. 한 장으로 보는 변화

```
[옛날 — 동기 DLL]  한 호출에 다 끝남
  VB.NET 앱 ──승인()──▶ KICC DLL ──▶ VAN ──▶ 카드사
           ◀──결과────              (블로킹: 그 자리에서 승인/실패)
  관심사: 거의 없음 (함수 호출 하나가 이음새 전부)

[현대 — 비동기 결제창 + 웹훅]  여러 단계·여러 시점·여러 주체
  ① 브라우저 ──결제창(PG SDK)──▶ PG    : 카드정보는 PG로 '직접' (우리 서버 안 거침)
  ② PG ──승인──▶ 카드사
  ③ PG ──웹훅(HTTP POST)──▶ 우리 서버  : "결제됨" 통지 (나중에, 비동기)
  ④ 우리 서버: 서명·멱등·금액 검증 → 주문 확정(PAID)
  관심사: 멱등성·웹훅검증·정합성·대사·보안 … 많음
```

**핵심:** 옛날은 결제가 *한 함수·한 순간·한 프로세스*라 architect할 게 없었다. 현대는 결제가 **분산·비동기 사건**(브라우저·PG·우리서버에 흩어짐)이 됐고, 아키텍처 관심사는 **전부 이 "흩어짐"에서 파생**된다.

### 왜 바뀌었나 (3가지 힘)
1. **데스크톱 → 웹**: 브라우저엔 DLL을 못 심음 → PG가 REST API + 결제창(JS SDK)으로 제공.
2. **보안(PCI-DSS)**: 카드정보를 우리가 받으면 컴플라이언스 범위 폭발 → **결제창으로 PG에 직접** 보내고 우리는 토큰만.
3. **규모·신뢰성**: 동시성·네트워크 단절·더블클릭 → 멱등성·재시도·대사 필수.

### 현대 관심사 (옛날 ↔ 현대 ↔ 우리 코드)
| 관심사 | 옛날 | 현대 | 우리 코드 |
|---|---|---|---|
| PG 추상화 | VAN DLL 받아 호출 | **포트**를 내가 정의, 어댑터 교체 | `PaymentGateway` ✅ |
| 결제 플로우 | 동기 `승인()` | **결제창 + 웹훅**(비동기) | Mock은 아직 동기 |
| 멱등성 | TID·거래번호 | `idempotencyKey`·이벤트ID 중복차단 | `idempotencyKey` unique ✅ |
| 정합성 | 앱이 결과 저장만 | 상태머신 + 금액검증 | `OrderStatus` PENDING/PAID ✅ |
| 대사/정산 | **VAN이 해줌** | 우리 DB ↔ PG 내역 직접 대조 | (확장 후보 — §다음) |
| 보안 | DLL이 카드 다룸 | 결제창+토큰화로 카드 비저장 | (모의라 해당없음) |

---

## 2. ★ 웹훅 흐름을 우리 코드에 그려보기

### 2.1 가장 큰 구조 변화: 방향이 뒤집힌다 (outbound → inbound 추가)

```
동기 모델:   우리 서버 ──approve()──▶ PG          (우리가 PG를 부름 = outbound)
웹훅 모델:   우리 서버 ◀──webhook────  PG          (PG가 우리를 부름 = inbound)  ← 새로 생김
```

- 지금 `PaymentGateway.approve()`는 **outbound 포트**(우리가 나감).
- 웹훅 엔드포인트는 **inbound 어댑터**(PG가 우리를 호출). 그래서 `PaymentController`에 **PG 전용 입구**가 하나 생기고, 인증이 **JWT가 아니라 서명검증**이 된다(PG는 우리 로그인 사용자가 아니므로).

> 옛날 동기 DLL엔 "통지를 기다린다"는 개념 자체가 없었다(호출=결과). 웹훅 = PG가 나중에 콜백하는 **비동기 통지** = 분산 시스템의 전형.

### 2.2 시퀀스 — 현재(동기) vs 웹훅(비동기)

```
[현재 — 동기]
  브라우저 ─▶ POST /api/payments ─▶ PaymentService.pay
                                      ├ gateway.approve()   (즉시 결과)
                                      └ orderService.pay()  (재고차감 + PAID)
           ◀─ 201 결제완료

[웹훅 — 비동기]
  1. 브라우저 ─▶ PG 결제창              (카드정보는 PG로 직접; 우리 서버 안 거침)
  2. PG ─▶ 카드사 승인
  3. PG ─▶ POST /api/payments/webhook ─▶ [① 서명검증][② 멱등수신][③ 금액검증]
                                          └ PaymentService.confirm (재고차감 + PAID)
        ◀─ 200 OK                        (200 안 주면 PG가 재시도)
  4. PG ─▶ 브라우저 redirect(successUrl) (UX용일 뿐 — 유실 가능 → 신뢰 X)
```

**왜 웹훅이 "진실의 원천"인가:** 4번 리다이렉트는 사용자가 창을 닫거나 네트워크가 끊기면 유실된다. 3번 웹훅은 **PG가 서버로 직접 + 재시도**하므로 신뢰할 수 있다. → 결제 확정은 **웹훅 기준**, 리다이렉트는 화면 전환용.

### 2.3 우리 코드 before/after (그림이라 실제 구현은 아님)

**PaymentController — inbound 엔드포인트 추가**
```java
// 현재 — 브라우저가 동기로 결제 (우리가 PG approve)
@PostMapping
public ResponseEntity<ApiResponse<PaymentResponse>> pay(@Valid @RequestBody PaymentRequest req) {
    return ...paymentService.pay(SecurityUtil.getCurrentMemberId(), req);
}

// 추가 — PG가 호출하는 웹훅 입구 (inbound 어댑터)
@PostMapping("/webhook")
public ResponseEntity<Void> handleWebhook(
        @RequestHeader("X-PG-Signature") String signature,  // PG가 동봉한 서명
        @RequestBody String rawBody) {                      // 원문(raw) — 서명검증하려면 String로 받아야
    paymentService.confirmFromWebhook(signature, rawBody);
    return ResponseEntity.ok().build();                     // 200 필수 (아니면 PG 재시도)
}
```

**SecurityConfig — 웹훅은 permitAll (서명으로 인증 대체)**
```java
// PG는 우리 로그인 사용자가 아님 → JWT 없음 → permitAll. 대신 ②.1의 서명검증이 '인증' 역할.
.requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
```

**PaymentService.confirmFromWebhook — 웹훅 3대 방어**
```java
public void confirmFromWebhook(String signature, String rawBody) {
    // ① 서명 검증 — 진짜 PG가 보낸 게 맞나(위조 차단). PG와 공유한 secret으로 HMAC 비교.
    if (!webhookVerifier.isValid(signature, rawBody))
        throw new BusinessException(HttpStatus.UNAUTHORIZED, "웹훅 서명 불일치");

    WebhookEvent e = parse(rawBody);   // { pgTransactionId, orderId, amount, status }

    // ② 멱등 수신 — 같은 통지가 두 번 와도 한 번만 처리(웹훅은 재시도되어 중복 도착!)
    if (paymentRepository.findByPgTransactionId(e.pgTransactionId()).isPresent())
        return;   // 이미 처리됨 → 조용히 200

    // ③ 금액 검증 — 통지 금액을 신뢰 X, 서버 '주문' 금액과 대조 (위변조 방어)
    //    ※ 웹훅은 '사용자'가 아니라 '시스템' 컨텍스트 → 소유권 검증 우회(서명검증이 그 자리를 대신)
    Order order = orderRepository.findById(e.orderId()).orElseThrow(...);
    if (order.getTotalPrice() != e.amount())
        throw new BusinessException(HttpStatus.BAD_REQUEST, "금액 불일치");

    // ④ 도메인 처리 — 기존 로직 재사용(재고차감 + Order/Payment PAID)
    //    Payment(READY→PAID) 저장 시 pgTransactionId 기록 → 다음 중복 웹훅을 ②가 걸러줌
    ...
}
```

### 2.4 멱등성이 두 겹이 된다
- **요청 멱등성**(이미 있음): `idempotencyKey` — 브라우저 더블클릭/재시도 방어.
- **웹훅 멱등성**(추가): `pgTransactionId`(또는 PG 이벤트ID) — PG 재시도로 같은 통지 중복 도착 방어.
- → `Payment`에 이미 `pgTransactionId` 컬럼이 있으니 `findByPgTransactionId`만 추가하면 자연스럽게 연결.

### 2.5 UX·트랜잭션 함의
- 재고 차감 시점이 **사용자 클릭 → 웹훅 도착**으로 이동. 사용자는 "결제 처리 중" 보고, 웹훅 확정 후 PAID. (폴링 또는 알림으로 화면 갱신)
- 웹훅 처리는 짧고 빠르게 끝내고(200 먼저), 무거운 후속(배송·알림)은 **이벤트/아웃박스로 비동기** 분리(§13.5) — 웹훅 핸들러에서 오래 끌면 PG가 타임아웃→재시도.

### 2.6 실무 의견으로 (페이로프)
- *"결제 확정은 리다이렉트 말고 **웹훅을 진실의 원천**으로. 멱등 수신 + 서명검증 필수."*
- *"웹훅은 200을 빨리 주고, 후속 처리는 이벤트로 분리 — 안 그러면 PG 재시도 폭주."*
- *"멱등을 두 겹으로: 요청은 idempotencyKey, 통지는 pgTransactionId."*
- *"웹훅 핸들러는 소유권 검증 대신 서명검증 — PG는 사용자가 아니라 신뢰된 시스템."*

---

## 3. 대사(reconciliation)·정산 — TID/거래번호의 진짜 쓸모

> 멘토 인사이트: *"정산은 시스템 중 제일 어렵다. 매출/금액이 회사에서 가장 중요하고, 회사마다 정산 방식이 달라서 DB 설계도 다 다르다."* — 맞다. 왜 그런지가 이 섹션.

### 3.1 용어 — 결제는 한 점이 아니라 타임라인이다
```
승인(authorization) → 매입(capture)   → 정산(settlement)       → 대사(reconciliation)
실시간 "한도 확보"     "청구 확정"        T+2~ 실제 입금(수수료     우리DB ↔ PG/카드사 내역
[우리 PAID 시점]       (승인과 동시 多)    차감) PG→가맹점 계좌      대조·검증 [보통 매일 배치]
```
옛날 동기 DLL은 **승인 한 점**만 앱이 다뤘고, 매입·정산·대사는 **VAN/카드사가 알아서** 해줬다. 현대 서버 시스템은 이 **타임라인 전체와 입금까지** 직접 책임지는 경우가 많다 → 그래서 어려워진다.

### 3.2 왜 정산이 제일 어려운가
1. **오차 허용 0** — 돈이라 1원도 안 맞으면 안 됨. 회계·세무·감사 대상.
2. **시간 차원** — 승인은 실시간, 정산은 T+N. "오늘 승인 100건"과 "3일 뒤 들어온 한 덩어리 입금"을 매칭해야 함(지연·부분·묶음).
3. **여러 진실의 출처** — 우리 DB / PG 리포트 / 카드사 명세 / 은행 입금. 넷이 수수료·환불·부분취소·망취소 때문에 서로 다름.
4. **케이스 폭발** — 부분환불, 망취소(승인 후 통신두절 자동취소), 익일취소, 할부, 쿠폰 부담주체, VAT, 카드사·상품별 상이한 수수료율.
5. **★ 회사마다 다름** — 매출 인식 시점, 정산 주기, 수수료 구조, 마켓플레이스면 판매자 2차 정산, 다중 PG/통화, ERP 회계연동까지. 비즈니스가 다르니 **정산 스키마가 천차만별** → "정답 스키마"가 없고 그 회사 비즈니스를 모델링하는 일.

### 3.3 대사 잡이 하는 일
```
매일: PG 정산파일/리포트(거래목록+정산액+수수료) 수신
  → 우리 DB(결제·환불)와 '거래키'(pgTransactionId / TID·승인번호)로 매칭
  → 불일치 분류:
       · 우리에만 있음   → 웹훅 유실·미통지 의심
       · PG에만 있음     → 우리 기록 누락
       · 금액 다름       → 수수료·부분취소·환불 반영 차이
       · 상태 다름       → 망취소·익일취소 등
  → 예외 큐로 빼서 자동 보정 or 사람이 처리
```

### 3.4 TID/거래번호의 진짜 쓸모 (당신 경험 연결)
그때 "잘 몰랐던" **TID(단말/가맹점 식별)·거래번호(승인번호)** = 대사의 **조인 키**였다. 우리 거래와 PG/카드사 거래를 잇는 식별자. 현대에선 `pgTransactionId`(우리 `Payment`에 이미 있음) + `idempotencyKey`가 그 역할 → 대사 잡이 이걸로 매칭한다.

### 3.5 우리 코드에 그려보면 — 정산은 '별도 도메인'
```
payment/  (거래 시점의 관심사)        settlement/  ← 새 도메인 (생명주기·관심사 다름)
  Payment: READY/PAID/CANCELLED        SettlementEntry: 거래키·정산액·수수료·정산일·정산상태
                                       ReconciliationJob: 일배치 (PG리포트 ↔ 우리DB 대조)
        pgTransactionId  ◀──조인키──   Mismatch: 불일치(우리만/PG만/금액상이) → 예외큐
```
- **왜 섞지 않나**: 결제(Payment)는 *"거래가 승인됐나"*, 정산(Settlement)은 *"며칠 뒤 수수료 떼고 얼마 입금됐나"* — **다른 시점·다른 관심사** → 다른 애그리거트로 분리(DDD 경계). 우리 `Payment`에 `fee`·`settledAt`을 욱여넣으면 결제 도메인이 정산 걱정까지 떠안아 오염된다.
- **매출 ≠ 결제액**: 결제 10,000원이라도 수수료·VAT·환불 빼면 실입금/매출 인식액이 다름 → 정산 모델이 이걸 1급으로 다뤄야 함.

### 3.6 왜 회사마다 DB가 다른가 (멘토 핵심)
정산 스키마는 **비즈니스 규칙의 직접 반영**이다: 언제를 매출로 인식? 정산 주기 며칠? 수수료 어떻게 분해? 마켓플레이스 2차 정산? 다중 PG·통화? ERP 연동? — 이게 회사마다 다르니 테이블이 다를 수밖에 없다. **결제 연동(PG 어댑터)은 표준화되지만, 정산은 그 회사 고유 도메인**이라 베껴올 수 없다. 그래서 가장 도메인 지식이 필요하고 가장 어렵다.

### 3.7 실무 의견으로 (페이로프)
- *"결제와 정산은 다른 도메인으로 분리하자 — 생명주기·관심사가 다르다."*
- *"대사 잡을 일 단위로: PG 리포트와 우리 DB를 거래키(pgTransactionId)로 매칭, 불일치는 예외 큐로 사람이 본다."*
- *"매출 = 결제액이 아니다. 수수료·VAT·환불을 정산 모델에 1급으로."*
- *"정산 스키마는 베껴오지 말고 우리 비즈니스(매출 인식·정산 주기·수수료)를 먼저 정의하고 모델링."*

---
*(이 노트는 단계별 누적 — 추후 토픽: 이벤트/아웃박스로 후속 디커플링, 다중 PG 전략 등)*
