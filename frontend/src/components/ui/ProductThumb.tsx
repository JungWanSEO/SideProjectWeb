"use client";

import { useState } from "react";

// 상품 썸네일. src(이미지 경로)가 있으면 <img>로 렌더하고, 없거나 로드 실패하면
// 이름 해시 기반 따뜻한 그라데이션 + 머리글자(세리프)로 우아하게 폴백한다.
// (placeholder 경로는 productImageSrc()가 결정 — 여기선 렌더링만 책임진다.)
const GRADIENTS = [
  "from-clay-100 to-clay-50",
  "from-sage-50 to-clay-50",
  "from-[#f0e6da] to-[#e7d9c7]",
  "from-[#efe7df] to-[#e4d3c4]",
  "from-[#eee3d6] to-[#e8d8c9]",
];

function hash(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) >>> 0;
  return h;
}

export default function ProductThumb({
  name,
  src,
  className = "",
}: {
  name: string;
  src?: string | null;
  className?: string;
}) {
  const [errored, setErrored] = useState(false);

  if (src && !errored) {
    return (
      // 로컬 SVG/외부 URL 모두 받으므로 next/image 대신 평범한 <img>(설정 의존 없음).
      // eslint-disable-next-line @next/next/no-img-element
      <img
        src={src}
        alt={name}
        loading="lazy"
        onError={() => setErrored(true)}
        className={`object-cover ${className}`}
      />
    );
  }

  // 폴백: "P01-Cap" → "Cap" → "C". 이름이 비슷해도 머리글자가 다양해지도록 대시 뒷부분을 쓴다.
  const gradient = GRADIENTS[hash(name) % GRADIENTS.length];
  const label = name.includes("-") ? name.slice(name.lastIndexOf("-") + 1) : name;
  const initial = (label.trim()[0] ?? "·").toUpperCase();
  return (
    <div className={`flex items-center justify-center bg-gradient-to-br ${gradient} ${className}`}>
      <span className="select-none font-serif text-4xl text-ink/25">{initial}</span>
    </div>
  );
}
