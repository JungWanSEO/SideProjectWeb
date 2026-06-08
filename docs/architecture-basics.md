# 아키텍처 기초 — 왜? (그림으로)

> Spring Boot 처음 보는 입장에서, 아키텍처가 **무엇이고 왜 존재하는지**를 바닥부터.
> "정의 외우기"가 아니라 **"이게 어떤 변경을 싸게 만들어주는가?"** 라는 질문으로 본다.
> 응용편(이걸 Payment 도메인에 적용·분석) → [payment-architecture-study.md](payment-architecture-study.md)
> 작성 2026-06-07.

---

## 0. 출발 질문: 아키텍처는 왜 존재하나

> **소프트웨어의 진짜 비용은 "처음 짜는 것"이 아니라 "나중에 바꾸는 것"이다.**

처음 만드는 건 한 번. 요구사항은 평생 바뀐다(PG 모의→토스, DB 교체, API 추가…).
**아키텍처 = 변경이 왔을 때 영향이 한 곳에 갇히도록 코드를 배치하는 기술.**

→ 모든 패턴을 볼 때 던질 질문: **"이게 어떤 변경을 싸게 만들려는 거지?"**

---

## 1. 모든 것의 적: 결합도(coupling)

**결합도 = A를 바꾸면 B도 바꿔야 하는 정도.** 높으면 "하나 고치니 열 군데 깨짐".
아키텍처의 목표 = 큰 덩어리를 의미 있는 조각으로 쪼개고 **조각 사이 결합을 최소화**.

---

## 2. 첫 도구: 계층 분리(Layered) — 왜 3층인가

```
HTTP 요청
   │
   ▼
┌────────────────┐
│  Controller    │  HTTP ↔ 객체 변환만
└───────┬────────┘
        ▼
┌────────────────┐
│  Service       │  비즈니스 규칙만 (결제 승인 → 재고 차감)
└───────┬────────┘
        ▼
┌────────────────┐
│  Repository    │  DB 접근만
└───────┬────────┘
        ▼
       DB
```

왜 이렇게 쪼개나? **"바뀌는 이유"가 다르기 때문**(단일 책임 원칙):

| 층 | 유일한 일 | 바뀌는 이유 |
|---|---|---|
| Controller | HTTP ↔ 객체 변환 | API 모양 바뀔 때 |
| Service | 비즈니스 규칙 | 정책 바뀔 때 |
| Repository | DB 접근 | DB·쿼리 바뀔 때 |

따로 오는 변경은 따로 있는 파일에 가둬야 서로 안 건드린다.
.NET 동일: Controller / Service / Repository(또는 DbContext).

**대부분의 앱은 이거면 충분.** 단, 여기 숨은 문제 하나가 고급 아키텍처들이 푸는 핵심이다. ↓

---

## 3. 진짜 핵심: 의존성의 "방향" ★ (이거 하나면 다 통함)

계층형에서 화살표(의존)는 기술 쪽(아래)으로 흐른다:

```
   Controller ──▶ Service ──▶ Repository ──▶ DB
                     │
                     └──▶ PaymentGateway구현(토스 API)
                          ▲ 기술이 바뀌면 Service가 흔들림
```

문제: **Service(비즈니스 핵심)가 Repository·PG(기술 세부)를 직접 가리킨다.**
그런데 가치의 순서는 거꾸로다:
- **비즈니스 규칙**("결제 승인되면 재고 깐다") = 오래 가는 핵심, 잘 안 변함
- **기술 세부**(토스 API 방식, DB 종류) = 자주 바뀌는 주변

핵심이 주변에 의존하는 건 거꾸로 → **의존성 역전(Dependency Inversion)**:
> 화살표를 뒤집어라. 핵심이 기술을 가리키지 말고, **기술이 핵심(인터페이스)을 가리키게** 하라.

도구는 **인터페이스**. 우리 `PaymentGateway`가 정확히 이걸 한다:

```
[나쁨] 핵심 ──▶ 기술 (화살표가 기술로)
   PaymentService
        │ 직접 의존
        ▼
   TossApiClient (구체 기술)            토스 바뀌면 → Service 수정 ✗


[좋음] 핵심 ──▶ «인터페이스» ◀── 기술 (화살표 뒤집힘) ← 지금 우리 코드
   PaymentService
        │ 의존
        ▼
   «interface» PaymentGateway
        ▲
        │ implements
   MockPaymentGateway (기술)            토스 어댑터 '추가'만 → Service 무수정 ✓
```

아래 그림에선 `PaymentService`가 토스/모의를 **모른다**(인터페이스만 앎). 기술이 인터페이스를 향해 화살표를 쏜다. **핵심이 무풍지대가 됐다.** 이것이 모든 고급 아키텍처의 심장.
.NET 비유: `IPaymentGateway` + DI. 동일.

---

## 4. 뒤집기를 실제로 돌리는 기계: DI / IoC (Spring의 심장)

화살표를 뒤집어도, 런타임에 "Service야, 진짜 구현 여기" 하고 **꽂아줄** 주체가 필요 → **DI/IoC 컨테이너**.

```java
@Service
public class PaymentService {
    private final PaymentGateway gateway;        // 인터페이스만 요구
    public PaymentService(PaymentGateway gateway) { this.gateway = gateway; }  // 생성자 주입
}
// @Component MockPaymentGateway 를 Spring이 찾아서 꽂아줌
```

`new`로 *내가* 의존을 만드는 게 아니라 *컨테이너*가 만들어 넣는다 = **IoC(제어의 역전)**.
.NET: `services.AddScoped<IPaymentGateway, TossGateway>()` + 생성자 주입. **완전 동일.**

→ **인터페이스(뒤집는 도구) + DI(뒤집힌 걸 연결하는 기계)** = 세트.

---

## 5. 아키텍처 패밀리 = 전부 "방향 뒤집기"의 변주

| 이름 | 그림 | 한 줄 본질 |
|---|---|---|
| **Layered** | 위→아래 | 책임은 나눴지만 화살표가 **기술 쪽으로** (3번 문제 잔존) |
| **Hexagonal** | 도메인 중앙·사방이 포트 | 모든 외부 접점을 **포트(인터페이스)로** → 화살표 전부 안으로 |
| **Onion** | 동심원 | 같은 원리를 **레이어 원**으로 |
| **Clean** | 동심원 + 역할 | 오니언 + UseCase/Entity 구분 더 명시 |

**셋(헥사고날·오니언·클린)은 같은 목표** = *"도메인이 바깥을 모르게, 모든 의존 화살표를 도메인 안으로"*. 이름·그림만 다르고 사상은 하나 = **의존성 역전(§3)**.

### 그림 ① 계층형 vs 헥사고날 — 패키지 구조

```
계층형 (지금)                      헥사고날 (한 도메인에 적용 시)
payment/                           payment/
├─ controller/                     ├─ adapter/
│   PaymentController              │   ├─ in/web/  PaymentController
├─ service/                        │   └─ out/
│   PaymentService                 │       ├─ persistence/ PaymentJpaAdapter
├─ repository/                     │       └─ pg/          TossGatewayAdapter
│   PaymentRepository              ├─ application/
├─ entity/                         │   ├─ PaymentService
│   Payment (JPA + HttpStatus)     │   └─ port/
└─ gateway/   ← 유일하게 포트로     │       ├─ in/  PayUseCase
    ├─ PaymentGateway     이미     │       └─ out/ SavePaymentPort, PaymentGatewayPort
    └─ MockPaymentGateway 끊음     └─ domain/
                                       Payment (순수 — JPA·HttpStatus 없음)
```
차이: 계층형은 **PG 한 곳만** 포트. 헥사고날은 **DB까지 전부** 포트 + 도메인을 기술에서 완전 분리.

### 그림 ② 헥사고날(육각형) — 화살표가 전부 안으로

```
        inbound 어댑터                         outbound 어댑터
   ┌────────────────────┐               ┌────────────────────────┐
   │ PaymentController   │   ──호출──▶   │   (도메인이 정의한 포트) │
   │ (HTTP를 도메인 호출)│               │   를 기술이 구현         │
   └────────────────────┘               │                         │
            │                            │  MockPaymentGateway      │
            ▼                            │  PaymentJpaAdapter        │
        ┌───────────────────────────┐   │        ▲ implements       │
        │     도메인 / 애플리케이션  │◀──┘────────┘                 │
        │  PaymentService + 결제규칙 │   포트(인터페이스)로만 의존    │
        │   바깥을 전혀 모름          │                              │
        └───────────────────────────┘
   ※ 모든 화살표가 가운데(도메인)로 향한다 = 도메인은 누가 부르는지·어디 저장되는지 모름
```

### 그림 ③ 오니언(양파) — 안쪽일수록 안 바뀜

```
   ┌─────────────────────────────────────────┐
   │  Infrastructure  (Web · DB · PG)         │  ← 가장 바깥 · 가장 자주 바뀜
   │   ┌───────────────────────────────────┐  │
   │   │  Application  (UseCase/오케스트레이션)│ │
   │   │   ┌─────────────────────────────┐ │  │
   │   │   │  Domain  (Payment, 상태머신) │ │  │  ← 가장 안 · 가장 안 바뀜
   │   │   └─────────────────────────────┘ │  │
   │   └───────────────────────────────────┘  │
   └─────────────────────────────────────────┘
        의존 화살표는 항상 바깥 → 안 (안은 바깥을 모른다)
```

차이는 "**어디까지** 뒤집느냐"의 정도:
- 우리: **PG 한 곳만** 포트(나머지는 계층형 그대로)
- 풀 헥사고날: **모든 외부 접점**을 포트로

---

## 6. 그럼 왜 항상 제일 좋은 걸(풀 헥사고날) 안 쓰나

뒤집기는 공짜가 아니다: 포트 파일 추가 + **도메인↔JPA 매핑 코드** + "정의가 두 군데".

> 판단 = **보호할 가치(도메인이 복잡·장수?) vs 바뀔 빈도(그 기술이 자주 바뀌나?)**

- PG: 진짜 바뀜(모의→토스) + 외부 경계 → 뒤집을 가치 ✅ (그래서 우리도 포트)
- 우리 DB: 바뀔 일 거의 없음 + 도메인 단순 → 뒤집으면 매핑 비용만 → **계층형 그대로가 합리적**

= "작은 서비스에 헥사고날 풀셋 = 오버 엔지니어링"의 정확한 의미.

---

## 한 문장 정리

> 아키텍처는 **변경을 싸게** 만드는 기술이고, 핵심 무기는 **의존성 방향을 도메인 안쪽으로 뒤집는 것**(인터페이스 + DI). 헥사고날·오니언·클린은 그 뒤집기를 **어디까지** 하느냐의 정도 차이일 뿐, 사상은 하나다.

### 우리 코드 좌표
- 계층형 + DDD-lite + **포트 1개(`PaymentGateway`)** = 이 규모에 적정.
- 더 뒤집고 싶을 때 후보(가벼운 것부터): `Payment` 엔티티의 `HttpStatus` 침투 걷어내기 → Payment만 헥사고날 리팩터. (상세 [payment-architecture-study.md](payment-architecture-study.md) §3·§5)
