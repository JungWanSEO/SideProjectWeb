# 동적 쿼리: Specification vs QueryDSL
상품 검색/필터를 어떻게 구현할까 — commerce-api 기술 의사결정

## 문제 — 왜 '동적 쿼리'인가
### 상품 검색/필터: 조건이 각각 선택적이다
- 키워드(`name`), 최소가, 최대가 — 들어올 수도, 안 들어올 수도
- 메서드명 파생 쿼리는 **조합 폭발**: `findByName...`, `...AndPriceBetween`, `...AndPriceGoe` …
- 조건이 런타임에 켜졌다/꺼졌다 → **정적 메서드로는 한계**
- 그래서 "조건을 동적으로 조립"하는 도구가 필요 → Specification 또는 QueryDSL

## 두 후보 한눈에
| 항목 | Specification | QueryDSL |
| 소속 | Spring Data JPA 내장 | 외부 라이브러리 |
| 기반 | JPA Criteria API | 자체 DSL(Q타입) |
| 의존성/빌드 | 추가 없음 | APT로 Q클래스 생성 |
| 가독성 | 장황한 편 | 좋음(LINQ 느낌) |
| 타입 안전 | 중간 | 강함(컴파일 체크) |
| 동적 조건 | Specification 조합 | BooleanBuilder/where 조합 |

## 코드 — Specification (JPA 내장)
### 추가 의존성 없이, 그러나 다소 장황
```
// repo: extends JpaSpecificationExecutor<Product>
static Specification<Product> search(String kw, Long min, Long max) {
  return (root, query, cb) -> {
    var ps = new ArrayList<Predicate>();
    if (kw  != null) ps.add(cb.like(root.get("name"), "%" + kw + "%"));
    if (min != null) ps.add(cb.ge(root.get("price"), min));
    if (max != null) ps.add(cb.le(root.get("price"), max));
    return cb.and(ps.toArray(new Predicate[0]));
  };
}
productRepository.findAll(search(kw, min, max), pageable);
```
> root.get("name")은 문자열 — 오타는 런타임에 발견된다.

## 코드 — QueryDSL (외부 라이브러리)
### Q타입으로 타입 안전, LINQ 같은 가독성
```
// product = QProduct.product (엔티티에서 자동 생성된 Q타입)
var where = new BooleanBuilder();
if (kw  != null) where.and(product.name.contains(kw));
if (min != null) where.and(product.price.goe(min));
if (max != null) where.and(product.price.loe(max));

List<Product> rows = queryFactory.selectFrom(product)
    .where(where)
    .offset(pageable.getOffset())
    .limit(pageable.getPageSize())
    .fetch();
```
> product.name 오타는 컴파일 단계에서 잡힌다.

## .NET 개발자 관점 (transfer)
- **QueryDSL ≈ LINQ to Entities** — `Where(p => p.Price >= min)`의 강타입·조립형 경험 그대로
- **Specification ≈ Expression 직접 조립** — IQueryable에 조건을 수동으로 쌓는 느낌
- 즉 LINQ에 익숙하면 **QueryDSL 진입장벽이 낮다** (개념 전이)
- 단, "익숙함"은 도입의 약한 근거 — 팀 컨벤션·유지보수 비용이 우선

## 한국 시장 현실
- QueryDSL은 한국 Spring 백엔드에서 **사실상 표준급 인기** (채용 우대사항 다수)
- 그러나 **MyBatis 진영도 거대** — SI·금융·공공은 SQL 직접 통제 선호
- 큰 갈림은 결국 **JPA/QueryDSL 진영 ↔ MyBatis 진영**
- "어떤 회사는 안 쓴다"가 맞다 → 도구 자체보다 **왜 골랐는지 설명력**이 중요

## 결정 기준 — commerce-api
| 상황 | 권장 |
| 조건 2~3개·의존성 최소화 | Specification |
| 동적 조건 많음·타입 안전·가독성 | QueryDSL |
| 학습/포폴 목적(전이 강점 살리기) | QueryDSL 한 번 경험 + 근거 기록 |
### 이 프로젝트의 잠정 선택
- 학습 목적 + .NET(LINQ) 전이 강점 → **QueryDSL로 검색/필터 구현 시도**
- 최종 결정과 근거는 `docs/dev-log.md`에 남긴다 (도구보다 의사결정 과정이 포트폴리오 자산)
