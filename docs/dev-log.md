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

- **보강**: ~~운영 하드닝(시크릿 env·Flyway)~~ ✅ → ~~인증 마무리(401/403·자동 refresh)~~ ✅ → ~~OAuth2 대비 Member prep(V2)~~ ✅ → ~~git init+GitHub~~ ✅(2026-06-05).
- 그 다음: **프론트엔드(React 개념·Next.js·디자인) 학습·폴리시**.
- (백엔드 후보) 결제(payment) 도메인 / 옵션 추가·수정 API / 카테고리 계층화.
