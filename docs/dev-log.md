# 개발 일지 (Dev Log) — 인덱스

> 추가 / 결정 / 문제·해결이 생길 때마다 기록한다.
> 목적: 면접용 근거(개발기간·결정·문제해결 스토리) + Claude 그라운딩.
> 작성법은 `dev-log` skill 참고.

**구조(2026-06-05~):** 길이 관리를 위해 상세 기록은 **월별 파일**로 분리하고, 이 파일은 **인덱스**(타임라인 요약 + 핵심 결정 + 다음 작업)만 유지한다. 새 기록은 **현재 월 파일에 append**하고, 이 인덱스에 한 줄 요약을 추가한다.

- 📂 [docs/dev-log/2026-05.md](dev-log/2026-05.md)
- 📂 [docs/dev-log/2026-06.md](dev-log/2026-06.md)

---

## 📅 타임라인 — 2026-05 · [상세 →](dev-log/2026-05.md)

- **05-31 (1일차) 프로젝트 세팅** — Initializr(Boot 3.5.14·Java 21·Gradle), 도메인형 패키지, 하네스 초석(CLAUDE.md·docs·skills). *문제: Boot 4.0 실수 설치 → 3.5.14 재생성.*
- **05-31 (1일차) member 도메인** — 가입/조회 + global 기반(BaseEntity·ApiResponse·전역예외). *문제: 한글 UTF-8 깨짐(셸 인코딩) → 400/500 교정.*

## 📅 타임라인 — 2026-06 · [상세 →](dev-log/2026-06.md)

**2일차 (06-02)**
- **member 테스트** — Service 단위 + Controller 슬라이스 (9개)
- **Repository 테스트** — @DataJpaTest, 테스트 피라미드 3층 (13개)
- **product 도메인** — 등록/조회. 가격 `long`·상태 enum·**애그리거트 간 ID 참조** 방침
- **product 테스트** — enum round-trip 검증 (23개)
- **order 도메인** — 생성/조회/취소, 가격 스냅샷, 재고 차감/복원 (36개)
- **cart 도메인** — 담기/조회/제거, order(스냅샷)와 대비되는 **라이브 참조** (48개)
- **MySQL 전환** — Docker MySQL 8(포트 **3307**), 테스트는 H2로 분리
- **API 문서화** — springdoc-openapi(Swagger)
- **인증 Phase 1** — Spring Security + JWT(STATELESS) + 역할 + BCrypt (53개)
- **인증 Phase 2** — Refresh 토큰 DB저장·회전(rotation) + `jti` 유일성 (58개)
- **모노레포 재구조** — `backend/` + `frontend/` 분리
- **재고 동시성** — `@Version` 낙관적 락 + spring-retry(새 트랜잭션), `OrderConcurrencyTest` (59개)
- **통합 시나리오 테스트** — 보안 필터 ON·실제 JWT E2E (60개)
- **상품 목록·페이징** — 커스텀 `PageResponse`, 기본 createdAt DESC (64개)

**3일차 (06-03)**
- **상품목록 런타임 검증 + 🚨 랜섬웨어 사고·복구** — 외부 노출 MySQL 침해 → 볼륨 폐기·비번 강화·포트 `127.0.0.1` 바인딩
- **내 주문 목록** — 본인 주문 페이징, N+1 = `default_batch_fetch_size` (68개)
- **주문 IDOR 보강** — 소유자/ADMIN 검증 없으면 403 (73개)
- **기술비교 슬라이드 파이프라인** — Markdown → PPTX 생성기(python-pptx)
- **상품 검색/필터** — QueryDSL 동적 where(키워드·가격대) (75개)
- **주문 요약 DTO** — 목록=요약 / 상세=전체
- **카테고리·브랜드 도메인 (2a)** — 대칭 도메인 신설 (87개)
- **카테고리·브랜드 ↔ Product (2b)** — Long FK + 검색필터 + 이름 enrich (88개)
- **상품 옵션(사이즈) 설계 합의** — 단일 축(사이즈), 색상=별도 상품
- **상품 옵션 P1** — 재고/`@Version`을 `ProductOption`으로 이동, `Product.stock` 제거, 주문 optionId 컷오버
- **옵션 P1 런타임 검증 + 레거시 컬럼 정리** — FREE 옵션 시드, 죽은 `stock`/`version` 컬럼 DROP(런타임 검증으로 발견)

**4일차 (06-04)**
- **옵션 P3 장바구니** — optionId 기준(같은 상품 다른 사이즈=별개 항목), 응답 size/stock/soldOut
- **프론트엔드 착수** — React+TS+Next.js 스택 결정, Next 15.1 스캐폴딩
- **FE 1차** — SecurityConfig CORS + 상품 목록 페이지(첫 FE↔BE 연동)
- **FE 2차** — 상품 상세(동적 라우트 `/products/[id]`)
- **인증 Phase 3** — JWT를 **httpOnly 쿠키**로 전환 + 로그인 UI (90개)
- **FE 장바구니** — 상세 담기 + `/cart` 페이지
- **체크아웃** — 서버 트랜잭션 방식 A(`POST /api/orders/checkout`) (93개)
- **FE 주문 목록·상세·취소** — 🎯 구매 전체 흐름 FE 완성
- **운영 하드닝** — 시크릿 OS env(12-factor) + Flyway 도입(V1, ddl `validate`)
- **인증 마무리** — 401 EntryPoint / 403 핸들러 + FE 자동 refresh
- **OAuth2 대비 Member prep** — Flyway **V2**(첫 실제 마이그레이션), provider/providerId

**5일차 (06-05)**
- **git/GitHub 정리 + 브랜드명 제거 + dev-log 월별 분리** — repo SideProjectWeb, main/dev + PR 워크플로(CONTRIBUTING.md), 무신사→패션 커머스
- **결제 도메인 설계 합의** — 모의 PG(포트-어댑터) · 재고 차감=결제 승인 시점(OrderStatus PENDING/PAID) · 멱등성 · Redis/MQ 확장지점 (상세 architecture.md §13)
- **결제 P1·P2** — payment 골격(상태머신·포트어댑터·V3) + 주문 흐름 전환(OrderStatus PENDING/PAID, 재고 차감→결제 시점, V4) (96 tests)
- **결제 P3** — `POST /api/payments`(모의 PG·멱등성), PaymentService 오케스트레이터(재고차감 위임), HTTP 구매 흐름 완성 (104 tests)

**6일차 (06-07)**
- **결제 P4 (취소·환불)** — 주문 취소 시 PG 환불 + Payment CANCELLED 연동. `PaymentGateway.refund` 포트, `PaymentService.cancelOrder`(결제→주문 한방향·@Transactional 원자성), 단일 취소 엔드포인트 유지 (107 tests). **MySQL 런타임 검증**(Flyway V1~V4·validate, 결제·환불 흐름 PASS)
- **FE 결제·취소 화면 (P5)** — `ORDERED` 기준이라 끊겨 있던 구매 흐름 복구. 결제 화면 신설(`/orders/[id]/pay`, 멱등키 `crypto.randomUUID`), 체크아웃→PENDING→결제→PAID→취소(환불), `OrderStatus` 3상태 동기화. 브라우저 E2E 검증 PASS
- **아키텍처 학습 노트(멘토 토픽)** — `docs/architecture-basics.md`(왜 아키텍처?·의존성 방향·DI·그림: 계층형 vs 헥사고날/오니언) + `docs/payment-architecture-study.md`(Payment 도메인에 적용한 before/after: Impl관습·헥사고날·Clean(HttpStatus 침투 위반)·Feign/MSA) + `docs/payment-modern-architecture.md`(옛날 동기 DLL→현대 결제: 웹훅 흐름을 우리 코드에 그림·서명/멱등/금액 3대 방어·inbound전환; 단계별 누적, 다음=대사/정산)

**7일차 (06-08)**
- **git 정리** — 결제 P1~P5 dev 병합(PR #2) 확인 + 아키텍처 노트 `docs`→dev 머지(`--no-ff`) + stale 브랜치(docs·feature/payment) 삭제. Claude가 git 직접 실행(머지/푸시) 위임 시작.
- **정산(Settlement) P1** — 결제 심화 착수. 새 도메인 `settlement/`(SettlementEntry: gross/fee/**net=실입금**·SCHEDULED→PAID_OUT·조인키 pgTransactionId), **배치 스캔**(PAID 결제→정산 항목, T+2)·수수료 2.5%·ADMIN API(run/list/payout)·Flyway V5·멱등(payment_id UNIQUE). "매출≠결제액" 1급 모델링 (112 tests). **MySQL 런타임 검증 PASS**(Flyway V5/validate·run/payout/멱등·ADMIN 403).
- **정산(Settlement) P2 — 대사(reconciliation)** — 두 진실의 출처(우리 정산 ↔ PG 리포트)를 `pgTransactionId`로 대조. `PaymentGateway.fetchSettlements()` 포트 + **상태 보유 Mock 원장**(독립 출처), `ReconciliationService`가 5분류(MATCHED/MISSING_IN_PG/MISSING_IN_OURS/AMOUNT_MISMATCH/STATUS_MISMATCH)→`Mismatch` 스냅샷 저장, ADMIN API(run/mismatches)·Flyway V6 (118 tests). **MySQL 런타임 검증 PASS**("정산 후 환불=STATUS_MISMATCH"·"정산 후 결제=MISSING_IN_OURS" 자연 발생).
- **정산(Settlement) P3 — 불일치 해소(resolve) 워크플로** — 예외 큐를 검출→처리까지. `Mismatch`에 `MismatchStatus`(OPEN→RESOLVED/IGNORED)+사유, `reconcile()`이 OPEN만 스냅샷·처리된 거래키는 재대사에서 안 깨움(`alreadyHandled`), ADMIN API(resolve/ignore·status 필터)·Flyway V7 (123 tests). **MySQL 런타임 검증 PASS**(resolve/ignore 후 재대사=total 0·alreadyHandled 2, 결정 보존).
- **FE 어드민 콘솔(정산·대사 화면)** — 스토어와 분리된 `/admin` 라우트 그룹 + 사이드바 셸. 정산 화면(배치 실행·입금 처리·gross/fee/net KPI), 대사 화면(대사 실행·불일치 테이블·resolve/ignore·상태 탭). 결정: **접근제어 3겹**(백엔드 hasRole=진짜 경계 / 프록시·WAF IP제한 / 프론트 게이팅=UX), 손수 Tailwind(shadcn 미도입). **브라우저 E2E PASS**.
- **이벤트·아웃박스 P1** — 결제완료를 **트랜잭셔널 아웃박스**로 안정 발행(dual-write 해소). 설계노트(`event-outbox-design.md`) + 구현: `OutboxEvent`/폴러(`@Scheduled`)/`EventPublisher` 포트/`PaymentCompletionRecorder`(결제저장+이벤트 한 tx)/알림 핸들러(`NotificationLog` event_id UNIQUE=멱등)·Flyway V8·V9 (130 tests). 결정: self-invocation 회피 위해 발행/폴러를 별도 트랜잭션 빈으로, at-least-once→멱등 소비. **MySQL 런타임 검증 PASS**(결제→outbox PENDING→폴러 PUBLISHED→알림, 재발행 멱등).
- **이벤트·아웃박스 P2a** — 폴러 신뢰성·스케일아웃 보강: **지수 백오프**(2→4→8→16s·`next_attempt_at`)+데드레터, **`FOR UPDATE SKIP LOCKED`** 행 클레임(다중 폴러 중복 발행 방지)·Flyway V10 (131 tests). 결정: 백오프 정책은 프로세서·엔티티는 저장만, SKIP LOCKED는 native(H2 미지원→MySQL 런타임으로). **MySQL 런타임 검증 PASS**(백오프 카운트다운→FAILED 데드레터·SKIP LOCKED 발행 경로).
- **다중 PG MPG-1** — 포트-어댑터를 어댑터 2개+라우터로 증명: **토스/카카오 모의 어댑터**(공통 `AbstractMockPaymentGateway` DRY) + **`PaymentGatewayRouter`**(provider 레지스트리·null→기본·미지원 400, 환불은 저장된 provider로 라우팅), `Payment.provider`·Flyway V11, 대사도 `fetchAllSettlements()` 집계로 전환 (136 tests). 결정: 라우팅=클라이언트 선택+레지스트리(페일오버는 스트레치), provider는 String. **MySQL 런타임 검증 PASS**(KAKAO/TOSS 결제·미지원 PG 400(행 안 남김)·환불 라우팅·2 PG 대사 집계).

**8일차 (06-09)**
- **다중 PG MPG-3 — 정산 PG별 수수료율** — `SettlementPolicy`를 provider별 요율 **Map**(TOSS 2.5%·KAKAOPAY 2.8%·폴백 3.0%)으로, `SettlementEntry`에 **provider + feeRate 스냅샷**(OrderItem 가격 스냅샷 패턴), 정산 결과에 **PG별 분해**(`byProvider`), Flyway V12 (138 tests). "매출≠결제액"에 **PG 차이**를 연결. 결정: 요율 출처=상수 Map(static util 유지), feeRate=double(돈 아닌 비율). **MySQL 런타임 검증 PASS**(같은 10,000원 → TOSS 250 vs KAKAOPAY 280, V12+validate, `fee_rate` double 저장).
- **다중 PG MPG-2 — 대사 PG별 분류/표시 강화** — 대사 불일치를 **어느 PG의 거래인지**로 분류·필터·표시. `PgSettlementRecord`에 `provider`(어댑터가 자기 `provider()` 기록 — 거래ID 프리픽스 `KAKAO-`≠provider `KAKAOPAY`라 프리픽스 파싱 불가), `Mismatch`에 provider(Flyway V13), `reconcile()`이 거래키별 provider 도출(우리=`SettlementEntry.provider`, PG측=리포트 provider)+**PG별 분해**(`byProvider`, 알파벳순), 불일치 목록에 **provider 필터**(`?provider=`, 대문자 정규화) (143 tests). **MySQL 런타임 검증 PASS**(2 PG 시나리오: TOSS matched+missingInOurs / KAKAOPAY statusMismatch, byProvider 분해·provider 필터·V13+validate).
- **다중 PG MPG-stretch — 라우터 페일오버** — 요청 PG가 장애(설정상 down)·승인 거절이면 **다른 PG로 자동 대체**해 결제 성공률 방어. `PaymentGatewayRouter.approveWithFailover()`가 요청 PG 먼저→실패 시 나머지 PG(알파벳순) 순차 시도, **실제 승인한 PG**를 `PaymentRoutingResult`로 반환해 Payment에 기록(환불도 그 PG로). `payment.unavailable-providers`(설정/env)로 점검 PG 지정. `PaymentService.pay`는 한 줄 교체(전략은 라우터에 가둠), Flyway 불필요 (149 tests). **MySQL 런타임 검증 PASS**(KAKAOPAY down → 요청 KAKAOPAY가 TOSS로 페일오버 승인·Payment.provider=TOSS, 미지원 PG 400, WARN 로그 관측). **오답노트 스킬 신설**(`.claude/skills/mistake-log/` — 운영 함정 기록·참고).

**9일차 (06-10)**
- **FE 다중 PG 노출 + 관리자 UX 정리** — 백엔드 다중 PG(MPG-1~3)를 화면으로. 결제 화면 **PG 선택**(토스/카카오페이→`provider` 전송), 어드민 정산 **PG·요율 컬럼 + PG별 분해**, 어드민 대사 **PG 컬럼 + PG 필터 + PG별 분해**, `lib/provider.ts`·`types.ts` 보강. UX: **관리자 로그인 시 정산 직행**(`login()`이 User 반환)·어드민 콘솔 **"스토어로" 링크 제거**. `tsc`/`next lint` 클린, 브라우저 E2E 확인. (FE는 테스트 없이 타입검사+브라우저 검증 — 기존 관례)
- **다중 PG 비용기반 라우팅** — `provider="AUTO"`면 **가장 싼 PG 자동 선택**, 페일오버도 **비용 오름차순**(싼 PG부터). **요율 출처 단일화**: `SettlementPolicy` 요율 Map 제거 → **`PaymentGateway.feeRate()`가 단일 출처**(요율=PG 고유 속성), 라우터 `feeRateOf()`로 노출, **정산이 라우터에서 요율을 읽음**(settlement→payment 정방향) → 라우팅 비용·정산 수수료 정의가 한 곳. FE 결제화면에 "자동(최저 수수료)" 옵션. (152 tests) **MySQL 런타임 검증 PASS**(AUTO→TOSS 최저가·정산 fee=780 무결성). **다중 PG 라우팅 3전략(클라이언트 선택/페일오버/비용기반) 완성.**
- **FE 웜 부티크 디자인 착수(WIP) + 제품 기획** — 기능 위주 최소 스타일 → **웜 부티크 디자인 시스템**(크림/점토/세이지 토큰·나눔명조/Pretendard·Button/Badge/ProductThumb·스토어프론트 전 화면 리디자인). `tsc`/`lint` 클린, **`feature/fe-design-polish`(`e16d10d`) 커밋·미머지**(디자인 더 다듬을 예정). + 이커머스 **deep-research 벤치마크**(패션 셀렉트샵) → **제품 기획 `docs/product-plan.md`**: 컨셉=*입점 브랜드 셀렉트샵(운영·정산 깊이 차별화)*, 다음=**Phase 1(상품 이미지→리뷰·평점→패션 필터 UI)**. ⚠️ 리서치 표본 무신사 편중·디자인 정량근거 미검증.

---

## 🧭 핵심 결정·이정표 (요약)

면접에서 자주 꺼낼 의사결정 모음. 상세 근거는 각 월 파일 + `docs/architecture.md`.

- **Boot 3.5 고정 (4.0 회피)** — 자료 풍부·안정. 마이그레이션은 단계 경유.
- **도메인형 패키지 + 애그리거트 간 ID 참조(DDD)** — 결합도↓·경계 명확·MSA 용이. 객체연관은 애그리거트 내부만.
- **재고 동시성 = `@Version` 낙관적 락 + 재시도** — 초과판매 0을 동시성 테스트로 증명. 재시도는 트랜잭션 바깥에서 새 트랜잭션.
- **동적 검색 = QueryDSL** — 타입 안전·.NET(LINQ) 전이. Specification 대비 가독성.
- **상품 옵션(사이즈)=SKU** — 재고/`@Version`을 옵션 단위로. 색상은 별도 상품(패션 셀렉트샵 모델).
- **인증 = JWT(httpOnly 쿠키)** — access/refresh 회전+`jti`, XSS(httpOnly)·CSRF(SameSite) 트레이드오프, 401/403 구분.
- **운영 하드닝 = 시크릿 OS env(12-factor) + Flyway** — `ddl-auto: validate`로 스키마는 마이그레이션이 통제.
- **모노레포 + FE React/TS/Next.js** — 풀스택 한 레포, C#→TS 시너지.
- **🚨 보안 사고 교훈** — 로컬 DB라도 외부 노출+약한 비번이면 자동 공격 대상 → `127.0.0.1` 바인딩.

---

## 다음 작업 (예정)

- **다중 PG 완주(06-08~10, dev 병합·런타임 검증)**: MPG-1(라우터)→MPG-3(정산 PG별 수수료율)→MPG-2(대사 PG별 분류)→MPG-stretch(라우터 페일오버)→**FE 노출**(결제 PG 선택·어드민 정산/대사 PG·요율 컬럼·관리자 UX) ✅(149 tests).
- **결제 심화 한 줄 완성**: 정산 기록(매출≠결제액)→대사(5분류)→불일치 해소(예외 큐)→운영 화면(어드민 콘솔)→결제완료 이벤트(트랜잭셔널 아웃박스)→폴러 신뢰성(백오프·SKIP LOCKED)→다중 PG(라우터→PG별 수수료율→PG별 대사→페일오버→화면).
- (다음 후보) 아웃박스 P2b(실제 RabbitMQ) / 대사 일자별 윈도우 / FE 디자인 폴리시 / 옵션 추가·수정 API / 카테고리 계층화 / dev→main 승격(마일스톤).
