// 다중 PG(provider) — 결제 시 선택지 + 라벨/뱃지.
// provider는 백엔드에서 열린 문자열(TOSS, KAKAOPAY, … — enum 아님)이라 라벨은 폴백(원문)을 둔다.

/** 결제 화면에서 고를 수 있는 PG (백엔드 지원: TOSS, KAKAOPAY). */
export const PROVIDERS = [
  { value: "TOSS", label: "토스페이먼츠" },
  { value: "KAKAOPAY", label: "카카오페이" },
] as const;

const LABELS: Record<string, string> = {
  TOSS: "토스페이먼츠",
  KAKAOPAY: "카카오페이",
};

/** 알 수 없는 provider도 원문으로 표시(폴백). */
export const providerLabel = (p: string) => LABELS[p] ?? p;

/** PG 뱃지 스타일 — provider가 늘어도 무난하도록 단일 중립 톤. */
export const PROVIDER_BADGE = "bg-indigo-50 text-indigo-700";

/** 수수료율(0.025) → "2.5%" 표시. */
export const formatRate = (rate: number) => `${(rate * 100).toFixed(1)}%`;
