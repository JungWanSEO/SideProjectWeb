import type { Config } from "tailwindcss";

/**
 * 디자인 시스템 토큰 — "웜 부티크" (크림 배경 · 세리프 헤딩 · 점토색 포인트).
 * 색/폰트/라운드/그림자를 여기서 한 번 정의하고 화면은 토큰만 쓴다(일관성·유지보수).
 */
export default {
  content: [
    "./src/pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/app/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        background: "var(--background)",
        foreground: "var(--foreground)",
        cream: "#faf7f2", // 페이지 배경(따뜻한 오프화이트)
        paper: "#fffdfb", // 카드/표면
        ink: "#2b2521", // 본문 텍스트(따뜻한 차콜)
        muted: "#908779", // 보조 텍스트
        line: "#e8e0d4", // 테두리/구분선
        clay: {
          DEFAULT: "#c06b4c", // 점토색(terracotta) — 주 포인트(CTA·링크·강조)
          50: "#f7ece5",
          100: "#ead3c6",
          600: "#ac5c3f",
          700: "#8e4a33",
        },
        sage: {
          DEFAULT: "#7c8a6f", // 세이지 — positive(실입금·재고·일치)
          50: "#eef1e9",
          600: "#66745a",
        },
        danger: "#b23a2e", // 경고/오류(따뜻한 레드)
      },
      fontFamily: {
        // 본문=Pretendard(한글 산세리프), 헤딩=나눔명조(한글 세리프) — globals.css에서 로드
        sans: ['"Pretendard Variable"', "Pretendard", "system-ui", "sans-serif"],
        serif: ['"Nanum Myeongjo"', "serif"],
      },
      borderRadius: {
        xl: "0.875rem",
        "2xl": "1.125rem",
      },
      boxShadow: {
        soft: "0 1px 2px rgba(43,37,33,0.04), 0 10px 30px -16px rgba(43,37,33,0.18)",
      },
    },
  },
  plugins: [],
} satisfies Config;
