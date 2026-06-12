"use client";

import { useState } from "react";

// 별점 표시(읽기 전용). 빈 별(라인색) 위에 채운 별(점토색)을 width%로 겹쳐 소수점까지 정밀 표현.
export default function Stars({
  value,
  className = "text-base",
}: {
  value: number;
  className?: string;
}) {
  const pct = Math.max(0, Math.min(100, (value / 5) * 100));
  return (
    <span
      className={`relative inline-block whitespace-nowrap leading-none ${className}`}
      aria-label={`평점 ${value.toFixed(1)} / 5`}
    >
      <span className="text-line">★★★★★</span>
      <span
        className="absolute inset-0 overflow-hidden text-clay"
        style={{ width: `${pct}%` }}
        aria-hidden
      >
        ★★★★★
      </span>
    </span>
  );
}

// 별점 입력(1~5). 호버/선택에 따라 채워진다.
export function StarInput({
  value,
  onChange,
  className = "text-2xl",
}: {
  value: number;
  onChange: (v: number) => void;
  className?: string;
}) {
  const [hover, setHover] = useState(0);
  const shown = hover || value;
  return (
    <span className={`inline-flex gap-1 ${className}`} role="radiogroup" aria-label="평점 선택">
      {[1, 2, 3, 4, 5].map((n) => (
        <button
          key={n}
          type="button"
          role="radio"
          aria-checked={value === n}
          aria-label={`${n}점`}
          onMouseEnter={() => setHover(n)}
          onMouseLeave={() => setHover(0)}
          onClick={() => onChange(n)}
          className={`leading-none transition ${n <= shown ? "text-clay" : "text-line"} hover:scale-110`}
        >
          ★
        </button>
      ))}
    </span>
  );
}
