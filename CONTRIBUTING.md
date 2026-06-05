# 기여 가이드 · Git 워크플로

commerce-api는 **브랜치 보호 + PR 병합** 흐름으로 작업합니다. 혼자 개발하더라도 실무 협업과 동일한 절차를 유지합니다.

## 🌿 브랜치 전략

```
main        보호 브랜치. 항상 빌드/테스트 통과하는 안정 상태. 직접 push 금지 — PR로만 병합.
 ▲ PR (릴리스 시점)
dev         통합 개발 브랜치. feature 들이 모이는 곳.
 ▲ PR
feature/*   기능·수정 단위. dev 에서 분기 → 작업 → PR 로 dev 에 병합.
```

| 브랜치 | 역할 | 예시 |
|--------|------|------|
| `main` | 안정/배포 | — |
| `dev` | 통합 작업 | — |
| `feature/*` | 기능 추가 | `feature/order-cancel` |
| `fix/*` | 버그 수정 | `fix/cart-stock` |

## 🔁 작업 흐름

```bash
# 1) dev 최신화
git switch dev
git pull

# 2) 기능 브랜치 분기
git switch -c feature/order-cancel

# 3) 작업 → 커밋 (커밋 규칙은 아래 참고)
git add .
git commit -m "feat(order): 주문 취소 API 추가"

# 4) 원격에 push
git push -u origin feature/order-cancel

# 5) GitHub에서 PR 생성 (base: dev) → 테스트 통과 확인 후 병합
# 6) 릴리스 시점에 dev → main PR
```

> PR 병합은 **Squash merge**를 권장합니다. feature 브랜치의 잡다한 커밋이 하나로 합쳐져 `dev` 히스토리가 깔끔해집니다.

## ✍️ 커밋 메시지 — Conventional Commits

`<type>(<scope>): <설명>` 형식을 따릅니다.

| type | 용도 |
|------|------|
| `feat` | 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변화 없는 구조 개선 |
| `test` | 테스트 추가/수정 |
| `docs` | 문서 |
| `chore` | 빌드·설정 등 기타 |

예시: `feat(product): 사이즈 옵션별 재고 검증 추가`

## ✅ PR 규칙

- 제목은 **무엇을/왜** 가 드러나게.
- `main` 병합 전 반드시 테스트 통과:
  ```bash
  cd backend
  ./gradlew test
  ```
- 백엔드 관련 명령은 모두 `backend/` 에서 실행합니다.
