# frontend

commerce 프로젝트의 프론트엔드 — 패션 커머스 웹.

## 스택 (의도적 선택 — 변경 시 사용자 확인)
- **Next.js 15.1** (App Router) · **React 19** · **TypeScript**
- **Tailwind CSS v3** (config 방식: `tailwind.config.ts`)
- 패키지 매니저: **npm**
- ⚠️ 최신 메이저(Next 16 / Tailwind v4) 대신 **15 / v3로 핀** — 강의·블로그·SO 자료가 풍부하고 안정적(백엔드 Boot 3.5 핀과 같은 철학). 근거는 `docs/dev-log.md`.

## 실행
```bash
npm install      # 최초 1회
npm run dev      # 개발 서버 → http://localhost:3000
npm run build    # 프로덕션 빌드
npm run lint     # ESLint
```

## 백엔드 연동
- API 서버: `../backend` (Spring Boot, `http://localhost:8080`)
- API 문서(계약): http://localhost:8080/swagger-ui/index.html
- ⚠️ 브라우저에서 API 호출 시 백엔드 `SecurityConfig`에 **CORS 허용**(`http://localhost:3000`) 필요.
- 인증: JWT(access + refresh 회전). FE 토큰 보관 전략(httpOnly 쿠키 vs 메모리)은 로그인 화면 작업 시 결정.

## 구조 (App Router)
```
src/app/
  layout.tsx        # 루트 레이아웃(공통 html/body, 폰트)
  page.tsx          # "/" 페이지
  globals.css       # Tailwind 디렉티브 + 전역 스타일
```
> 경로 = 폴더 구조. 예: `src/app/products/page.tsx` → `/products`, `src/app/products/[id]/page.tsx` → `/products/1`.

## 비고 (Node 버전)
- 현재 Node v20.10.0. ESLint 9 일부 패키지가 Node ≥20.19를 권장(`npm run dev`는 무관, `lint`/`build`에서 경고 가능) → 추후 Node 20.19+/22 LTS 업데이트 권장.
