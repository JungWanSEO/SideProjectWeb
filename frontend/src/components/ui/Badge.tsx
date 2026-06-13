import { ReactNode } from "react";

// 디자인 시스템 뱃지 — 톤별 색. 상태/PG/품절 등에 일관되게 쓴다.
type Tone = "neutral" | "clay" | "sage" | "danger" | "dark";

const TONE: Record<Tone, string> = {
  neutral: "bg-line/60 text-muted",
  clay: "bg-clay-50 text-clay-700",
  sage: "bg-sage-50 text-sage-600",
  danger: "bg-danger/10 text-danger",
  dark: "bg-ink text-cream",
};

export default function Badge({
  tone = "neutral",
  children,
  className = "",
}: {
  tone?: Tone;
  children: ReactNode;
  className?: string;
}) {
  return (
    <span className={`inline-flex items-center rounded-full px-2.5 py-0.5 text-xs font-medium ${TONE[tone]} ${className}`}>
      {children}
    </span>
  );
}
