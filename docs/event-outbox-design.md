# 이벤트·아웃박스 설계 (학습/설계 노트)

> 목표: **결제 완료를 안정적으로 "이벤트"로 발행**해 후속(알림·배송·정산 디커플링)을 비동기로 분리한다.
> `architecture.md §13.5`가 예고한 *"결제완료 이벤트 발행 + 아웃박스 패턴(DB 커밋↔메시지 발행 원자성)"*의 구체 설계.
> 관련: [payment-modern-architecture.md](payment-modern-architecture.md)(웹훅·대사) · [architecture-basics.md](architecture-basics.md)(의존성 방향).
> 작성 2026-06-08. **설계 단계 — 코드 전에 결정 확정용.**

---

## 1. 문제 — dual-write (두 번 쓰기)

결제가 완료되면 두 가지를 해야 한다:
1. **DB 상태 변경** — 결제 PAID 저장
2. **외부 통지(이벤트 발행)** — 알림·배송·정산 등 후속을 깨우기

문제: 이 둘은 **서로 다른 시스템**(우리 DB ↔ 메시지 브로커)이라 한 트랜잭션으로 못 묶는다.

```
[순진한 방법 A] DB 커밋 → MQ 발행
   DB 커밋 ✅ → (그 순간 크래시) → MQ 발행 ❌   ⇒ 이벤트 유실 (후속이 영영 안 일어남)

[순진한 방법 B] MQ 발행 → DB 커밋
   MQ 발행 ✅ → (DB 롤백) ❌                    ⇒ 유령 이벤트 (없던 일을 통지)
```

즉 "상태 변경"과 "이벤트 발행"의 **원자성**이 깨진다. 돈이 걸린 결제에서 이건 치명적이다.

---

## 2. 패턴 — 트랜잭셔널 아웃박스(Transactional Outbox)

> 핵심 한 줄: **이벤트를 밖으로 바로 쏘지 말고, "같은 DB 트랜잭션 안에서" outbox 테이블에 한 줄 INSERT 한다.**

```
[쓰기 트랜잭션 — 하나의 로컬 DB 트랜잭션]
   payment 상태=PAID 저장   ┐
   outbox INSERT(이벤트)     ┘  ← 둘이 한 커밋 = 원자적 (둘 다 되거나 둘 다 안 되거나)

[발행 — 비동기, 별도]
   폴러(Poller)가 outbox의 PENDING 행을 읽어 발행 → PUBLISHED 표시
   발행 실패하면 PENDING으로 남음 → 다음 tick에 재시도  ⇒ at-least-once 보장
```

- **원자성**: 상태 변경과 이벤트 기록이 *같은 DB 한 트랜잭션*이라 절대 어긋나지 않는다(분산 트랜잭션 불필요).
- **신뢰성**: 발행은 폴러가 책임. 크래시·MQ 다운이어도 outbox에 남아 있으니 복구 후 재발행 → **유실 없음(at-least-once)**.
- **디커플링**: 결제 코드는 "outbox에 기록"까지만. 후속(알림·배송)은 핸들러가 따로.

**.NET 비유**: MediatR 도메인 이벤트 + (EF Core / MassTransit) **트랜잭셔널 아웃박스**. "SaveChanges에 도메인 이벤트도 같은 트랜잭션으로 outbox에 넣고, 백그라운드 디스패처가 발행"하는 바로 그 패턴.

---

## 3. 왜 다른 방법은 부족한가 (대안 비교)

| 방법 | 한계 |
|---|---|
| **`@TransactionalEventListener(AFTER_COMMIT)`** (Spring 네이티브) | "커밋 후 발행"이라 *커밋 ~ 리스너 실행 사이 크래시*면 이벤트 **유실**. durability(영속) 없음 → 아웃박스가 이걸 메우는 것. |
| **2PC / XA 분산 트랜잭션** | DB와 MQ를 한 분산 트랜잭션으로 묶음. 무겁고·느리고·MQ 지원 약하고·운영 복잡 → 사실상 회피. |
| **결제 코드에서 직접 MQ 호출** | dual-write 문제 그대로(§1). |
| **CDC(Debezium 등)** | outbox 테이블 변경을 로그(binlog)에서 캡처해 발행 — 폴러조차 없앰. 강력하지만 인프라 무거움 → 확장 경로(§8). |

→ 아웃박스 = **로컬 DB 트랜잭션 하나만** 쓰고, 발행은 폴러로 분리. 가장 단순하면서 유실 없음.

---

## 4. 우리 시스템에 그려보기

### 4.1 발행 지점 — 결제 완료

현재 `PaymentService.pay`는 의도적으로 `@Transactional`이 없다(내부 `orderService.pay`의 낙관락 재시도를 보존하려고). 그래서 `payment.save`가 자기 트랜잭션으로 따로 커밋된다 — 바로 여기 주석에 적어둔 갭:
> *"주문 PAID 커밋과 결제 PAID 저장 사이의 원자성은 단순화 — 운영에선 이벤트/아웃박스로 보강."*

**설계**: 결제 완료(`payment.markPaid` + `save`)와 **outbox INSERT**를 묶는 **작은 `@Transactional` 메서드**로 추출한다. 재시도가 필요한 `orderService.pay`는 그대로 바깥에 둔다.

```
// before
orderService.pay(orderId);            // 별도 트랜잭션 + 재시도 (재고 차감 + 주문 PAID)
payment.markPaid(pgTxId);
paymentRepository.save(payment);      // 자기 트랜잭션

// after
orderService.pay(orderId);            // (그대로 — 바깥)
recordPaidWithEvent(payment, pgTxId); // ★ @Transactional: payment.save + outbox.save 원자적
```

```
@Transactional
void recordPaidWithEvent(Payment payment, String pgTxId) {
    payment.markPaid(pgTxId);
    paymentRepository.save(payment);
    outboxService.append("PAYMENT_COMPLETED",
        payload(orderId, paymentId, amount));   // 같은 트랜잭션 INSERT
}
```

### 4.2 소비 지점 — 폴러 + 핸들러

```
@Scheduled poller (예: 2초)
   → outbox에서 PENDING N건 조회(생성순)
   → 각 이벤트를 EventDispatcher로 디스패치
        → 핸들러(들)가 후속 처리 (알림·배송·…)
   → 성공: status=PUBLISHED / 실패: retry_count++ 후 PENDING 유지
```

실제 MQ는 아직 없으니 **in-process 디스패치**로 시뮬레이션하되, **어댑터 경계**(`EventPublisher` 포트)를 둬서 나중에 RabbitMQ/Kafka 어댑터로 교체할 수 있게 한다(포트-어댑터, `PaymentGateway`와 같은 사고방식).

---

## 5. 스키마 — `outbox_event`

| 컬럼 | 타입 | 의미 |
|---|---|---|
| `id` | bigint PK | 발행 순서 키(생성순) |
| `event_type` | varchar(50) | 예: `PAYMENT_COMPLETED` |
| `aggregate_type` | varchar(50) | 예: `PAYMENT` (라우팅·디버깅용, 선택) |
| `aggregate_id` | varchar(50) | 예: paymentId (선택) |
| `payload` | JSON/text | 이벤트 본문 `{orderId, paymentId, amount, …}` |
| `status` | enum(`FAILED`,`PENDING`,`PUBLISHED`) | 알파벳순(Hibernate ENUM ↔ Flyway validate) |
| `retry_count` | int default 0 | 재시도 횟수 |
| `created_at` | datetime(6) | 생성 시각 |
| `published_at` | datetime(6) null | 발행 완료 시각 |
| `last_error` | varchar(255) null | 마지막 실패 사유(디버깅, 선택) |

- **status 모델**: `PENDING`(미발행) → `PUBLISHED`(성공) / `FAILED`(최대 재시도 초과 = 데드레터).
- (선택, P2) `next_attempt_at` 컬럼으로 **백오프**(지수 백오프 재시도) — P1은 단순 재시도로 시작.

---

## 6. 시퀀스

```
브라우저 ─결제─▶ PaymentService.pay
                 ├ orderService.pay()            (별도 tx·재시도: 재고차감+주문PAID)
                 └ recordPaidWithEvent() ──┐
                      payment PAID 저장      │ 한 트랜잭션(원자적 커밋)
                      outbox INSERT(PENDING) ┘
        ◀─201────
                                         ┄┄┄┄ (이후, 비동기) ┄┄┄┄
   @Scheduled 폴러 tick ─▶ outbox PENDING 조회(생성순)
                          ─▶ EventDispatcher ─▶ PaymentCompletedHandler (후속)
                          ─▶ 성공: PUBLISHED / 실패: retry_count++ (PENDING 유지)
```

---

## 7. 신뢰성 설계 결정 (핵심)

1. **at-least-once → 소비자 멱등성 필수**: 폴러가 "핸들러 성공 후 PUBLISHED 표시" 직전에 크래시하면, 다음 tick에 **같은 이벤트를 또 디스패치**한다. 그래서 **핸들러는 멱등**해야 한다 — 방법: ① 핸들러 자체가 멱등(예: upsert), 또는 ② 소비자 측 "처리한 event id" 기록 후 중복 스킵. (P1 핸들러가 알림 로그면, `event_id` 유니크로 중복 무시.)
2. **순서**: outbox `id` 오름차순으로 발행 → 생성 순서 유지. **단일 폴러** 가정. 스케일아웃(폴러 여러 개)이면 `SELECT … FOR UPDATE SKIP LOCKED`로 행 잠금 분할(경쟁/중복 방지) — P2.
3. **재시도/데드레터**: 실패 시 `retry_count++` 후 PENDING 유지(다음 tick 재시도). `retry_count >= MAX`면 `FAILED`로 보내 무한 재시도 차단(데드레터 = 사람이 본다). 백오프는 P2(`next_attempt_at`).
4. **정리(보관)**: `PUBLISHED` 행이 무한정 쌓이므로 주기적 아카이브/삭제(또는 짧은 TTL) — 운영 항목, P2.
5. **폴러 주기**: 너무 짧으면 DB 부하, 길면 지연. 학습용 2초 정도. (실무는 보통 수백 ms~수초 + 배치 크기.)

---

## 8. 확장 경로 (이음새만 확보)

```
[지금] 결제 → outbox(MySQL) → @Scheduled 폴러 → in-process 핸들러
[다음] 폴러가 EventPublisher 포트로 발행 → RabbitMQ/Kafka 어댑터로 교체 (소비자도 분리 서비스)
[더]   CDC(Debezium)가 outbox binlog를 캡처해 발행 → 폴러조차 제거 (인프라 무거움)
```

- 포트 `EventPublisher`(우리가 정의) + 어댑터(`InProcessEventPublisher` → 추후 `RabbitEventPublisher`). 결제·정산 코드는 포트에만 의존 → 교체에 코드 변경 0(`PaymentGateway`와 동형, [architecture-basics.md] DIP).
- 멱등 소비·재시도·데드레터는 그대로 MQ 세계에서도 통한다(오히려 더 필요).

---

## 9. 이 프로젝트의 결정 (코드 들어가기 전 확정)

| 항목 | 추천(기본) | 비고 |
|---|---|---|
| **발행 지점** | 결제 완료(`recordPaidWithEvent` @Transactional) | §4.1 |
| **이벤트** | `PAYMENT_COMPLETED` 하나로 시작 | 추후 늘림 |
| **핸들러(소비)** | **알림 단일 핸들러** → `NotificationLog`(event id 유니크 = 멱등 소비) | §7.1 |
| **발행 방식** | in-process + `EventPublisher` 포트(어댑터 경계) | 추후 MQ |
| **재시도** | P1=단순 재시도(retry_count++/PENDING) + MAX→FAILED | 백오프=P2 |
| **멱등 소비** | event id 유니크/핸들러 멱등 | §7.1 |
| **순서/동시성** | 단일 폴러·생성순 | SKIP LOCKED=P2 |
| **마이그레이션** | Flyway **V8**(`outbox_event`) | |
| **테스트** | 아웃박스 원자 기록 / 폴러 디스패치 / 실패 재시도 / 멱등 | |

**P1 범위(제안)**: outbox 테이블 + 결제완료 원자 기록 + `@Scheduled` 폴러 + `EventPublisher` 포트 + 핸들러 1개 + 단순 재시도 + 테스트. (백오프·SKIP LOCKED·보관·MQ는 P2 이후.)

---

## 10. 실무 의견으로 (페이로프)

- *"결제 완료 이벤트는 커밋 후 발행 말고 **트랜잭셔널 아웃박스**로 — 같은 트랜잭션 INSERT라 유실이 구조적으로 불가능."*
- *"`@TransactionalEventListener`만으로는 부족하다(커밋~리스너 사이 크래시 유실). durability가 핵심."*
- *"발행은 at-least-once니까 **소비자는 멱등**하게 — 중복은 정상 시나리오다."*
- *"폴러는 포트 뒤에 두자 — 지금 in-process여도 RabbitMQ로 갈 때 결제 코드는 안 바뀐다."*
- *"실패는 무한 재시도 말고 N회 후 **데드레터**로 빼서 사람이 본다(대사의 예외 큐와 같은 사고)."*

---

## 11. P1 구현 체크리스트 (확정)

핸들러 = **알림 단일 핸들러** 확정. 만들 것:
- `OutboxEvent`(entity)·`OutboxStatus`(enum FAILED/PENDING/PUBLISHED)·repository + **Flyway V8**(`outbox_event`)
- `EventPublisher`(포트) + `InProcessEventPublisher`(어댑터, 핸들러로 디스패치)
- `OutboxService.append(...)` + `PaymentService.recordPaidWithEvent`(@Transactional: payment.save + outbox.append)
- `@Scheduled` 폴러(`OutboxRelay`) — PENDING 조회→발행→PUBLISHED / 실패 retry_count++·MAX→FAILED. `@EnableScheduling`
- `NotificationLog`(entity, `event_id` UNIQUE=멱등) + `PaymentCompletedHandler`(알림 기록) + **Flyway V9**(또는 V8에 합본)
- 테스트: 아웃박스 원자 기록 / 폴러 발행→PUBLISHED / 실패 재시도→FAILED / 중복 디스패치 멱등

*(설계 확정 완료 — 다음 단계: P1 구현.)*
