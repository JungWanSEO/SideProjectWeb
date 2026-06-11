import { ButtonHTMLAttributes } from "react";

// 디자인 시스템 버튼 — 점토색 pill. Link에도 쓰도록 buttonClass()를 따로 노출한다.
type Variant = "primary" | "secondary" | "ghost";
type Size = "sm" | "md" | "lg";

const VARIANT: Record<Variant, string> = {
  primary: "bg-clay text-cream hover:bg-clay-600 disabled:opacity-50",
  secondary: "border border-line bg-paper text-ink hover:border-clay hover:text-clay disabled:opacity-50",
  ghost: "text-muted hover:text-ink",
};

const SIZE: Record<Size, string> = {
  sm: "px-3 py-1.5 text-xs",
  md: "px-5 py-2.5 text-sm",
  lg: "px-6 py-3.5 text-base",
};

export function buttonClass(variant: Variant = "primary", size: Size = "md", extra = ""): string {
  return `inline-flex items-center justify-center gap-2 rounded-full font-medium transition disabled:cursor-not-allowed ${VARIANT[variant]} ${SIZE[size]} ${extra}`;
}

export default function Button({
  variant = "primary",
  size = "md",
  className = "",
  ...props
}: { variant?: Variant; size?: Size } & ButtonHTMLAttributes<HTMLButtonElement>) {
  return <button className={buttonClass(variant, size, className)} {...props} />;
}
