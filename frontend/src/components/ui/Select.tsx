"use client";

import { Listbox, ListboxButton, ListboxOption, ListboxOptions } from "@headlessui/react";

export interface SelectOption {
  value: string;
  label: string;
}

/**
 * 드롭다운(콤보박스) — Headless UI `Listbox` 기반.
 *
 * 닫힌 버튼도, **펼친 패널도** 브랜드에 맞춰 직접 스타일(둥근 모서리·웜 토큰)하면서,
 * 키보드(↑↓·Home/End·type-ahead·Esc)·스크린리더(ARIA)·포커스·뷰포트 위치는 라이브러리가 처리한다.
 * (네이티브 select의 "공짜 접근성"을 디자인 자유와 함께 가져오는 Headless 경로 — deep-research 결론.)
 */
export default function Select({
  value,
  onChange,
  options,
  placeholder = "선택",
  className = "",
}: {
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
  placeholder?: string;
  className?: string;
}) {
  const selected = options.find((o) => o.value === value);

  return (
    <Listbox value={value} onChange={onChange}>
      <div className="relative">
        <ListboxButton
          className={`group inline-flex items-center justify-between gap-2 rounded-full border border-line bg-paper py-2.5 pl-4 pr-3 text-sm text-ink transition hover:border-clay focus:outline-none focus-visible:ring-2 focus-visible:ring-clay/40 data-[open]:border-clay ${className}`}
        >
          <span className="truncate">{selected?.label ?? placeholder}</span>
          <svg
            className="h-4 w-4 shrink-0 text-muted transition group-data-[open]:rotate-180"
            viewBox="0 0 20 20"
            fill="none"
            stroke="currentColor"
            strokeWidth="1.6"
            aria-hidden
          >
            <path d="M6 8l4 4 4-4" strokeLinecap="round" strokeLinejoin="round" />
          </svg>
        </ListboxButton>

        <ListboxOptions
          anchor="bottom start"
          transition
          className="z-30 mt-2 max-h-72 w-[var(--button-width)] min-w-44 overflow-auto rounded-2xl border border-line bg-paper p-1.5 shadow-soft focus:outline-none data-[closed]:scale-95 data-[closed]:opacity-0"
        >
          {options.map((o) => (
            <ListboxOption
              key={o.value}
              value={o.value}
              className="group flex cursor-pointer items-center justify-between gap-2 rounded-xl px-3 py-2 text-sm text-ink transition data-[focus]:bg-clay-50 data-[selected]:font-medium data-[selected]:text-clay-700"
            >
              <span className="truncate">{o.label}</span>
              <svg
                className="h-4 w-4 shrink-0 text-clay opacity-0 group-data-[selected]:opacity-100"
                viewBox="0 0 20 20"
                fill="none"
                stroke="currentColor"
                strokeWidth="1.8"
                aria-hidden
              >
                <path d="M5 10l3.5 3.5L15 6" strokeLinecap="round" strokeLinejoin="round" />
              </svg>
            </ListboxOption>
          ))}
        </ListboxOptions>
      </div>
    </Listbox>
  );
}
