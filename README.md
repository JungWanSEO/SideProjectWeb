# commerce-api · 패션 커머스 백엔드

> 의류·패션 셀렉트샵 스타일의 이커머스 서비스를 모델로 한 **백엔드 클론 프로젝트**.
> .NET(ASP.NET Core) 응용프로그램 개발자가 **Java / Spring Boot 백엔드로 전환**하며 만든 학습·포트폴리오용 모노레포입니다.

<p>
  <img alt="Java" src="https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white">
  <img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.5.14-6DB33F?logo=springboot&logoColor=white">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-8-4479A1?logo=mysql&logoColor=white">
  <img alt="JWT" src="https://img.shields.io/badge/Auth-JWT-000000?logo=jsonwebtokens&logoColor=white">
  <img alt="QueryDSL" src="https://img.shields.io/badge/QueryDSL-5.1.0-0769AD">
  <img alt="Flyway" src="https://img.shields.io/badge/Flyway-migration-CC0200?logo=flyway&logoColor=white">
  <img alt="Next.js" src="https://img.shields.io/badge/Next.js-React%20TS-000000?logo=nextdotjs&logoColor=white">
</p>

---

## 📌 프로젝트 소개

실무에서 **C# / ASP.NET Core**로 응용프로그램을 개발하던 경험을 바탕으로, **Spring Boot 생태계의 백엔드 설계**를 직접 구현하며 익히는 것을 목표로 합니다. 단순 CRUD가 아니라 **인증·동시성·동적 검색·스키마 마이그레이션** 같은 실서비스에서 마주치는 문제를 의도적으로 다뤘습니다.

- 🎯 **목표**: 이커머스 백엔드의 핵심 흐름(회원 → 상품/검색 → 장바구니 → 주문)을 처음부터 설계
- 🧩 **방식**: 도메인형 패키지 구조 · 테스트 주도 · 의사결정을 [개발 일지](docs/dev-log.md)로 기록
- 🏗 **구조**: `backend`(Spring Boot) + `frontend`(Next.js) **모노레포**

---

## 🛠 기술 스택

| 구분 | 기술 |
|------|------|
| Language / Runtime | Java 21 |
| Framework | Spring Boot **3.5.14** (의도적으로 3.5 선택 · 4.0 미사용) |
| Persistence | Spring Data JPA · Hibernate · **QueryDSL 5.1.0**(동적 쿼리) |
| Database | MySQL 8 (Docker) · 테스트는 H2 |
| Migration | **Flyway** (버전관리된 SQL로 스키마 통제) |
| Security | Spring Security · **JWT**(jjwt 0.12.6, access/refresh) |
| Docs | springdoc-openapi (Swagger UI) |
| Ops | Actuator · Lombok · Validation |
| Frontend | React · TypeScript · Next.js *(진행 중)* |

---

## 📁 모노레포 구조

```
SideProjectWeb/
├── backend/                  # Spring Boot 애플리케이션
│   └── src/main/java/com/commerce/api/
│       ├── auth/             # 인증 (JWT 발급/검증, 로그인)
│       ├── member/           # 회원
│       ├── product/          # 상품 + 옵션(사이즈)/재고
│       ├── category/         # 카테고리
│       ├── brand/            # 브랜드
│       ├── cart/             # 장바구니
│       ├── order/            # 주문
│       └── global/           # 공통 설정·예외·시큐리티 (인프라)
├── frontend/                 # Next.js (React + TS) — 진행 중
└── docs/                     # 아키텍처/개발 일지
```

각 도메인은 `controller · service · repository · entity · dto` 로 일관되게 분리했습니다. (ASP.NET Core의 Controller/Service/Repository 계층 분리와 동일한 의도)

---

## ✨ 핵심 기능

- **인증/인가** — Spring Security + JWT (access 30분 / refresh 14일), 비밀번호 해시 저장
- **상품 검색** — 카테고리·브랜드·키워드 등 **동적 조건 필터링** (QueryDSL)
- **상품 옵션** — 사이즈별 옵션 단위로 재고 관리 (같은 상품 다른 사이즈 = 별개 항목)
- **장바구니** — 옵션 단위 담기, 응답에 size/stock/품절 여부 포함
- **주문** — 주문 생성 시 재고 차감, 주문 요약 DTO 응답
- **API 문서** — Swagger UI 자동 생성

---

## 🧠 기술적 의사결정 & 도전

> 포트폴리오의 핵심 — "무엇을"보다 **"왜 그렇게 결정했는지"**.

#### 1. Spring Boot 3.5 고정 (4.0 미사용)
출시 직후의 4.0 대신 **레퍼런스·트러블슈팅 자료가 풍부한 3.5 LTS 라인**을 의도적으로 선택. 학습 단계에서 프레임워크 자체의 미성숙 이슈에 시간을 뺏기지 않기 위함.

#### 2. 스키마는 Flyway가, 검증만 Hibernate가
`ddl-auto: validate` + **Flyway 마이그레이션**(`V1__init`, `V2__add_oauth_fields`)으로 스키마를 통제. Hibernate `update` 자동 생성에 의존하지 않고, **버전관리된 SQL로 운영 스키마를 재현 가능**하게 만들었습니다. 엔티티↔스키마 불일치 시 기동 실패 = 의도된 안전장치.

#### 3. 재고 동시성 — 낙관적 락(@Version)
주문이 몰릴 때의 **재고 갱신 경합**을 낙관적 락으로 처리하고, 이를 **동시성 통합 테스트**(`OrderConcurrencyTest`)로 검증. 재고/버전을 상품이 아닌 **상품 옵션 단위**로 분리해 사이즈별로 독립적으로 다루도록 설계.

#### 4. 동적 검색 — QueryDSL
조건이 있을 때만 `where` 절에 붙는 **타입 안전한 동적 쿼리**로 검색 필터를 구현. 문자열 JPQL 조립의 휴먼에러를 피했습니다. (C#의 LINq + EF Core 표현식과 유사한 경험)

#### 5. N+1 완화
목록 조회의 N+1을 `default_batch_fetch_size`(IN 절 묶음 로딩)와 fetch join으로 완화.

#### 6. 시크릿 외부화 (12-factor)
DB 비밀번호·JWT secret을 코드/깃이 아닌 **OS 환경변수**로 주입. 값이 없으면 **기동 실패**하도록 두어 설정 누락을 조기에 드러냅니다.

> 더 자세한 근거는 [docs/architecture.md](docs/architecture.md), 진행 기록은 [docs/dev-log.md](docs/dev-log.md) 참고.

---

## 🚀 실행 방법

> 모든 백엔드 명령은 `backend/` 디렉터리에서 실행합니다.

```bash
cd backend

# 1) 환경변수 준비 (.env.example 참고)
#    MYSQL_USER, MYSQL_PASSWORD, JWT_SECRET 등을 .env 에 작성
cp .env.example .env   # Windows: copy .env.example .env

# 2) MySQL 기동 (Docker)
docker compose up -d

# 3) 애플리케이션 실행
./gradlew bootRun       # Windows(env 자동 로드): ./run.ps1
```

| 항목 | 주소 |
|------|------|
| API 서버 | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| MySQL (Docker) | localhost:**3307** / db `commerce` |

---

## 🧪 테스트

```bash
cd backend
./gradlew test
```

- 테스트 환경은 **H2 인메모리 DB**로 분리 (운영 MySQL과 독립)
- 단위 + 컨트롤러(slice) + 통합 + **동시성** 테스트, 현재 **93개** (@Test 기준)

---

## 📚 더 보기

- [아키텍처 설계 근거](docs/architecture.md)
- [개발 일지 (의사결정·문제해결 기록)](docs/dev-log.md)
