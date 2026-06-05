---
name: add-domain
description: commerce-api에 새 도메인(기능 단위)을 추가할 때 사용. 도메인형 패키지 컨벤션(controller/service/repository/entity/dto)에 맞춰 패키지 구조와 기본 뼈대를 만든다. 예: payment, review, coupon, category 등 새 도메인을 추가할 때.
---

# 새 도메인 추가 절차

commerce-api는 **도메인형(package-by-feature)** 구조다. 새 도메인 `<domain>`을 추가할 때 아래 순서를 따른다.

## 1. 패키지 생성
`com.commerce.api.<domain>` 아래 5계층 하위 패키지를 만든다:
```
<domain>/
├── controller/
├── service/
├── repository/
├── entity/
└── dto/
```

## 2. 계층별 작성 순서 (안 → 밖)
1. **entity** — DB 테이블 매핑. `@Entity`, PK는 `@Id @GeneratedValue(strategy = IDENTITY)`. 생성/수정일시 등 공통 필드는 `global/common`의 BaseEntity를 상속.
2. **repository** — `interface <Domain>Repository extends JpaRepository<<Entity>, Long>`
3. **dto** — 요청/응답을 분리 (예: `<Domain>CreateRequest`, `<Domain>Response`). **엔티티를 컨트롤러에 직접 노출하지 않는다.**
4. **service** — `@Service`. **생성자 주입만 사용** (`private final` 필드 + Lombok `@RequiredArgsConstructor`). 비즈니스 로직을 여기 둔다.
5. **controller** — `@RestController`, `@RequestMapping("/api/<domains>")`. 요청 DTO는 `@Valid`로 검증.

## 3. 컨벤션 체크리스트
- [ ] 생성자 주입만 사용 (필드 주입 `@Autowired` 금지)
- [ ] 컨트롤러는 DTO만 주고받음 (엔티티 노출 금지)
- [ ] 검증 어노테이션(`@NotNull`, `@Size` 등)은 요청 DTO에 부착
- [ ] 도메인 간 의존은 service 계층을 통해서만
- [ ] **다른 애그리거트(도메인) 참조는 객체 연관(@ManyToOne) 대신 ID(Long) 참조**. 객체 연관은 같은 애그리거트 내부에서만. (근거: `docs/architecture.md`)
- [ ] 금액은 `long`(원), enum은 `@Enumerated(EnumType.STRING)`

## 4. 마무리 (필수)
- `./gradlew build` 로 컴파일 통과 확인
- **`dev-log` skill 형식에 따라 `docs/dev-log.md`에 "추가" 항목 기록**