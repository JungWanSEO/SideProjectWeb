# Payment 도메인으로 보는 아키텍처 (학습 노트)

> 멘토가 짚어준 현업 토픽을 **이 프로젝트의 Payment 도메인 실제 코드**에 적용해 본 분석.
> 교과서 정의가 아니라 *우리 코드 → 이렇게 바꾸면 → 이런 장단점*의 before/after로 본다.
> **기초편(왜 아키텍처? 의존성 방향·DI·그림)** → [architecture-basics.md](architecture-basics.md) 먼저 보면 이 문서가 선명해진다.
> 작성 2026-06-07. 기준 코드: `com.commerce.api.payment.*`

다루는 토픽:
1. `interface + Impl` 관습 — 왜 줄었나 / 작은 서비스에서 굳이?
2. 헥사고날(포트 & 어댑터)
3. Clean · Onion 아키텍처
4. Feign client / MSA (완전 도메인 분리)
5. 종합 — 우리 규모엔 뭐가 맞나

---

## 0. 현재 Payment 구조 (기준점)

```
payment/
├─ controller/PaymentController        ← inbound (HTTP)
├─ service/PaymentService              ← 오케스트레이션 (concrete class, no interface)
├─ repository/PaymentRepository        ← Spring Data JPA interface
├─ entity/Payment, PaymentStatus       ← JPA 엔티티 + 상태머신 행위(cancel/markPaid…)
├─ dto/PaymentRequest, PaymentResponse
└─ gateway/                            ← ★ 아웃바운드 포트-어댑터 (이미 존재!)
   ├─ PaymentGateway (interface=포트)
   ├─ MockPaymentGateway (구현=어댑터)
   └─ PaymentApproval, PaymentRefund (결과 VO)
```

의존성 방향: `Controller → Service → (Repository, PaymentGateway 포트)` / `Service → OrderService`(인-프로세스).

**한 줄 요약:** 실용적 **레이어드** + **DDD-lite**(애그리거트 ID 참조) + **아웃바운드 포트 1개**(PaymentGateway). 즉 "풀 헥사고날"은 아니지만 가장 중요한 외부 경계(PG) 한 곳만 포트로 끊어둔 상태. — 이게 우리 규모에 합리적인 절충이라는 게 이 문서의 결론(§5).

---

## 1. `interface + Impl` 관습 — 왜 줄었나

### 옛날 스타일
```
FooService (interface)
FooServiceImpl (구현)   ← 서비스마다 기계적으로 1:1
```

### 왜 예전엔 거의 강제였나
- **Spring AOP 프록시**가 과거엔 JDK 동적 프록시 → **인터페이스가 있어야** @Transactional 등 프록시가 걸렸다.
- 테스트에서 mock 만들기 쉬웠다(인터페이스 기반).

### 왜 지금은 굳이 안 하나
- Spring Boot는 **CGLIB 클래스 프록시가 기본**(`proxyTargetClass=true`) → 인터페이스 없이도 `@Transactional`·AOP 동작. **PaymentService에 @Transactional이 그냥 붙어서 도는 것**이 증거.
- **Mockito가 클래스도 목**한다 → 테스트 위해 인터페이스 만들 필요 없음. (`PaymentServiceTest`가 `@InjectMocks PaymentService` 콘크리트로 잘 됨)
- 구현이 **하나뿐인데** 인터페이스를 만들면 → 파일 2배 + "정의가 두 군데"라 추적만 번거로움(보일러플레이트).

### 원칙 (현업 합의)
> **인터페이스는 "구현이 2개 이상"이거나 "진짜 경계를 끊어야 할 때"만 만든다.**
> 미래에 생길지 모를 교체를 위해 미리 인터페이스화 = YAGNI 위반.

### 우리 코드에 적용
| 위치 | 인터페이스? | 판정 |
|---|---|---|
| `PaymentService` | ❌ 콘크리트 | ✅ 정답 — 구현 1개, 프록시·목 다 됨 |
| `PaymentGateway` | ✅ 인터페이스 + `MockPaymentGateway` | ✅ 정답 — 구현이 **2개 될 예정**(Mock→실제 PG) + **외부 경계** |
| `ProductRepositoryImpl` | ✅ 유일한 `Impl` | ⚠️ 이건 **스타일이 아니라 프레임워크 강제** ↓ |

**`ProductRepositoryImpl`이 유일한 Impl인 이유:** QueryDSL 동적 쿼리를 Spring Data 리포에 끼우려면 *커스텀 프래그먼트* 패턴을 써야 하고, 그 구현체 이름은 **반드시 `<리포명>Impl`**이어야 Spring Data가 찾는다(네이밍 규약). 즉 "내가 Impl 관습을 따른 것"이 아니라 **Spring Data가 그 이름을 요구**한 것. → 면접 포인트: *"Impl은 거의 안 쓰는데, QueryDSL 커스텀 리포는 프레임워크가 이름을 강제해서 그것만 Impl이다."*

---

## 2. 헥사고날 (포트 & 어댑터)

### 개념
도메인을 가운데 두고, 바깥과의 모든 접점을 **포트(인터페이스)** 로 정의 → 실제 기술은 **어댑터**가 구현.
- **inbound(주도) 포트/어댑터**: 바깥이 앱을 *호출*하는 쪽 (컨트롤러, 테스트).
- **outbound(피주도) 포트/어댑터**: 앱이 바깥을 *호출*하는 쪽 (DB, 외부 API).

### 우리는 이미 한 조각을 하고 있다
`PaymentGateway`(포트) ↔ `MockPaymentGateway`(어댑터) = **교과서적인 아웃바운드 포트-어댑터**.
`PaymentService`는 토스인지 모의인지 모르고 인터페이스에만 의존 → 운영 전환 시 **어댑터만 갈아끼움**(DIP). .NET `IPaymentGateway` + DI와 동형.

### "풀 헥사고날"이면 뭐가 더 바뀌나
지금은 **PG만** 포트다. 완전 헥사고날이면 **DB 접근도 포트**가 된다:

**Before (현재)** — 서비스가 Spring Data 리포에 직접 의존
```java
@Service
public class PaymentService {
    private final PaymentRepository paymentRepository; // Spring Data(JPA) 인터페이스에 직접 의존
}
```

**After (헥사고날)** — 도메인이 정의한 포트 + JPA 어댑터
```
payment/
├─ domain/            Payment(순수), PaymentStatus            ← JPA 애노테이션 없음
├─ application/
│   ├─ PaymentService                                       ← 포트에만 의존
│   └─ port/out/ LoadPaymentPort, SavePaymentPort           ← 도메인이 '필요'를 선언
└─ adapter/
    ├─ in/web/  PaymentController
    └─ out/persistence/
        ├─ PaymentJpaEntity (JPA)                           ← 영속 모델 분리
        ├─ PaymentPersistenceAdapter implements …Port       ← 포트 구현
        └─ PaymentJpaRepository (Spring Data)
```
```java
// application/port/out/SavePaymentPort.java  — 도메인 언어로 "필요"를 정의
public interface SavePaymentPort { Payment save(Payment payment); }

// adapter/out/persistence/PaymentPersistenceAdapter.java — JPA로 그 필요를 충족
@Component
class PaymentPersistenceAdapter implements SavePaymentPort, LoadPaymentPort {
    private final PaymentJpaRepository jpa;
    public Payment save(Payment p) { return jpa.save(PaymentJpaEntity.from(p)).toDomain(); }
}
```

### 장단점
| | 장점 | 단점 |
|---|---|---|
| 헥사고날(풀) | 도메인이 기술(JPA·HTTP·PG)과 **완전 분리** → 교체·테스트 쉬움, 도메인 순수 | **매핑 보일러플레이트 폭증**(도메인↔JPA 변환), 파일 수 2~3배, 작은 앱엔 인지비용↑ |

### "유지보수 문제"의 양면 (멘토 메모)
- **큰/오래 가는 시스템**: 인프라가 자주 바뀌어도 도메인이 안 흔들림 → **유지보수 유리**.
- **작은 서비스**: 바뀔 일 없는 DB를 위해 포트·어댑터·매퍼를 다 만드는 건 **오버 엔지니어링** → 오히려 유지보수 부담. 멘토의 *"헥사고날은 완전 도메인 급(MSA)으로 갈 때"* 와 같은 맥락.

---

## 3. Clean · Onion 아키텍처

### 한 가족
Onion(Palermo) · Clean(Uncle Bob) · Hexagonal — 셋 다 **"의존성은 바깥→안(도메인)으로만 흐른다"** 는 같은 원리(의존성 역전). 강조점만 다름:
- **Hexagonal**: 대칭적 포트/어댑터(좌우 inbound/outbound).
- **Onion**: 동심원 레이어(Domain → Application → Infrastructure).
- **Clean**: 위 + use case/entity 역할 구분을 더 명시.

### 핵심 = 의존성 규칙
> 안쪽(도메인)은 바깥쪽(웹·DB·프레임워크)을 **몰라야** 한다.

### ★ 우리 코드의 위반 — 도메인이 HTTP를 안다
**Payment 엔티티(도메인 코어)가 웹 개념(HttpStatus)을 import 한다:**
```java
// entity/Payment.java (현재)
import org.springframework.http.HttpStatus;          // ← 웹/전송 계층 개념이 도메인에!

public void cancel() {
    requireStatus(PaymentStatus.PAID);
}
private void requireStatus(PaymentStatus expected) {
    if (this.status != expected) {
        throw new BusinessException(HttpStatus.CONFLICT,  // ← 도메인이 "409"를 안다
            "결제 상태 전이가 올바르지 않습니다. ...");
    }
}
```
Onion/Clean 관점에선 **레이어 위반**이다. "결제 상태가 틀렸다"는 **도메인 규칙**인데, 그걸 `409 CONFLICT`라는 **HTTP 전송 결정**과 묶어버렸다. PG를 콘솔 앱이나 배치에서 재사용하면 HTTP가 의미 없는데도 끌려온다.

**After (의존성 규칙 준수)** — 도메인은 도메인 예외만, HTTP 매핑은 바깥에서
```java
// domain — HTTP 모름
public class InvalidPaymentStateException extends RuntimeException { … }

public void cancel() {
    if (this.status != PaymentStatus.PAID)
        throw new InvalidPaymentStateException(this.status, PaymentStatus.PAID);
}

// adapter/in/web — 바깥(전송 계층)에서 도메인 예외 → HTTP 코드로 번역
@ExceptionHandler(InvalidPaymentStateException.class)
ResponseEntity<…> handle(InvalidPaymentStateException e) {
    return status(HttpStatus.CONFLICT).body(...);   // 409는 여기서 결정
}
```
> 같은 위반이 `Order.cancel()`·`OrderProcessor` 등 다른 엔티티에도 있음(공통 `BusinessException(HttpStatus,…)` 패턴). 우리는 **의도적으로 단순화**해서 도메인에 HttpStatus를 허용한 것 — 트레이드오프를 "알고" 택한 것과 "모르고" 택한 것은 다르다.

### 왜 쓰나 / 단점
- **왜**: 프레임워크·DB·전송 방식이 바뀌어도 도메인 규칙은 무풍. 도메인만 단위 테스트(스프링 없이). 신규 인원이 "핵심 규칙"을 한곳에서 읽음.
- **단점**: 도메인 예외 ↔ HTTP 매핑, 도메인 모델 ↔ JPA 매핑 등 **번역 레이어**가 늘어 작은 앱엔 과함.

---

## 4. Feign client / MSA — 완전 도메인 분리

### 현재 = 인-프로세스 호출
```java
// PaymentService.cancelOrder (현재) — 같은 JVM, 같은 트랜잭션에 합류 가능
orderService.cancel(orderId, memberId, admin);   // 그냥 메서드 호출
```
`@Transactional`로 **주문 취소 + 결제 환불을 한 트랜잭션**으로 묶을 수 있는 건 *둘이 같은 프로세스·같은 DB*이기 때문(우리가 P4에서 원자성 택한 근거).

### MSA라면 = 네트워크 호출 (Feign)
Payment 서비스 / Order 서비스가 **따로 배포 + 각자 DB**가 되면, 위 한 줄이 HTTP 호출이 된다:
```java
// Feign = 선언적 HTTP 클라이언트. 인터페이스만 선언하면 Spring이 구현 생성.
@FeignClient(name = "order-service", url = "${order.url}")
public interface OrderClient {
    @PostMapping("/api/orders/{id}/cancel")
    OrderResponse cancel(@PathVariable Long id);
}

// 호출부는 거의 똑같아 보이지만…
orderClient.cancel(orderId);   // 실제론 HTTP 요청 (네트워크!)
```

### 그러면 무엇이 깨지나 (분산 시스템의 대가)
- **분산 트랜잭션 불가**: `@Transactional`은 한 DB만 묶는다. 결제 DB 커밋과 주문 DB 커밋을 원자적으로 못 한다 → **사가(Saga) / 아웃박스 / 보상 트랜잭션 / 최종 일관성**이 필요. (우리가 `architecture.md §13.5`에 미리 적어둔 "이벤트/아웃박스" 확장지점이 **바로 이 지점**)
- **네트워크 실패·타임아웃·재시도·서킷브레이커**(Resilience4j 등) 필요.
- 운영 복잡도(서비스 디스커버리, 추적, 버전 호환) 급증.

### 우리는 이미 MSA "준비"가 돼 있다
DDD-lite 원칙 — **애그리거트 간 객체 연관 없이 ID로만 참조**(`Payment.orderId : Long`, `@ManyToOne` 아님). 이게 MSA 분리를 쉽게 만든다: 객체 그래프가 도메인 경계를 넘지 않으니, 경계를 따라 **그대로 쪼개면** 됨. 지금 한 모놀리식이 "모듈러 모놀리스"라서 나중에 서비스로 떼기 좋은 상태.

### 장단점
| | 장점 | 단점 |
|---|---|---|
| MSA + Feign | 독립 배포·독립 확장(결제만 스케일아웃), 팀 분리, 장애 격리 | 분산 트랜잭션·네트워크·운영 복잡도 폭증. **"작은 서비스에 굳이?"** = 대부분 No |

---

## 5. 종합 — 우리 규모엔 뭐가 맞나

### 의사결정 표
| 상황 | 적정 아키텍처 |
|---|---|
| 1~2인, CRUD 중심, 단일 배포 | **레이어드 + DDD-lite** (지금 우리) |
| 외부 연동(PG·메일 등) 있는 곳 | 그 **경계만 포트-어댑터** (지금 PaymentGateway 한 곳) ✅ |
| 도메인 규칙 복잡·오래 갈 핵심 | 그 도메인만 **헥사고날/Clean** 부분 적용 |
| 팀·트래픽이 도메인별로 갈림 | **MSA + Feign/이벤트** |

### 결론
- 지금 구조(레이어드 + ID참조 + 포트 1개)는 **이 규모에 과하지도 모자라지도 않다.** 멘토 말대로 *"작은 서비스에서 헥사고날 풀셋은 오버 엔지니어링"*.
- **단, 트레이드오프를 알고 택해야 한다.** 예: §3의 `HttpStatus` 도메인 침투는 "알고 한 단순화". 면접에서 *"의존성 규칙상 위반이지만 규모상 의도적으로 허용했고, 커지면 도메인 예외+핸들러로 분리한다"* 라고 말할 수 있으면 그게 실력.

### 학습용 다음 스텝 (선택)
- Payment **한 도메인만** §2 After처럼 `domain/application/adapter` 로 리팩터 → 헥사고날 체감(과하게 전파 X).
- 또는 §3 위반(HttpStatus)만 도메인 예외 + `@ExceptionHandler` 로 걷어내기 → 가장 적은 비용으로 "의존성 규칙" 체험.

### 면접 한 줄 요약 (토픽별)
- **Impl**: "구현 1개면 인터페이스 안 만든다. CGLIB·Mockito로 충분. QueryDSL 커스텀 리포만 Spring Data가 이름을 강제해 Impl."
- **헥사고날**: "외부 경계(PG)만 포트-어댑터로 끊었다. 풀셋은 이 규모엔 오버."
- **Clean/Onion**: "의존성은 도메인 안쪽으로. 우리 코드의 HttpStatus 도메인 침투가 대표적 위반이고, 커지면 걷어낸다."
- **MSA/Feign**: "ID 참조 DDD로 분리 준비는 됐다. 인-프로세스 호출이 Feign이 되는 순간 분산 트랜잭션→사가/아웃박스가 필요해진다."
