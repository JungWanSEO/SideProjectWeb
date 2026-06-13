// 상품 이미지 src 결정.
// - 백엔드 imageUrl이 있으면 그걸 그대로 사용(진짜 상품 이미지가 들어오면 우선).
// - 없으면(더미/시드 상품) 로컬 placeholder로 폴백. public/products/ 에 웜 부티크 톤으로 직접 그린
//   의류 일러스트(SVG)가 있다. 진짜 사진(.jpg)을 같은 폴더에 넣고 imageUrl을 채우면 자연스럽게 교체된다.
//
// 폴백 선택 순서:
//   1) 상품명에 의류 키워드가 있으면 그에 맞는 일러스트(예: "P01-Cap" → cap.svg) — 데모가 자연스러워진다.
//   2) 못 찾으면 상품 id로 "결정적으로" 하나(같은 상품 = 항상 같은 그림).

const PLACEHOLDERS = [
  "/products/tee.svg",
  "/products/dress.svg",
  "/products/cap.svg",
  "/products/sneaker.svg",
  "/products/bag.svg",
  "/products/jacket.svg",
  "/products/pants.svg",
  "/products/hoodie.svg",
  "/products/sunglasses.svg",
  "/products/sweater.svg",
  "/products/skirt.svg",
  "/products/scarf.svg",
] as const;

// 상품명 키워드 → 일러스트. 위에서부터 먼저 매칭되는 것을 쓴다(한/영 모두 커버).
const BY_KEYWORD: { kw: string[]; file: string }[] = [
  { kw: ["cap", "hat", "모자", "캡"], file: "/products/cap.svg" },
  { kw: ["hood", "후디", "후드"], file: "/products/hoodie.svg" },
  { kw: ["jacket", "coat", "재킷", "자켓", "코트"], file: "/products/jacket.svg" },
  { kw: ["sneaker", "shoe", "스니커", "운동화", "신발"], file: "/products/sneaker.svg" },
  { kw: ["sweater", "knit", "니트", "스웨터"], file: "/products/sweater.svg" },
  { kw: ["dress", "원피스", "드레스"], file: "/products/dress.svg" },
  { kw: ["skirt", "스커트", "치마"], file: "/products/skirt.svg" },
  { kw: ["scarf", "muffler", "스카프", "머플러", "목도리"], file: "/products/scarf.svg" },
  { kw: ["sunglass", "glasses", "선글라스", "안경"], file: "/products/sunglasses.svg" },
  { kw: ["bag", "tote", "가방", "백팩"], file: "/products/bag.svg" },
  { kw: ["pants", "trouser", "바지", "팬츠", "슬랙스"], file: "/products/pants.svg" },
  { kw: ["tee", "shirt", "티셔츠", "셔츠", "티"], file: "/products/tee.svg" },
];

/** 상품의 대표 이미지 경로. imageUrl 우선 → 이름 키워드 → id 기반 결정적 placeholder. */
export function productImageSrc(p: { id: number; name?: string; imageUrl?: string | null }): string {
  if (p.imageUrl) return p.imageUrl;

  const name = (p.name ?? "").toLowerCase();
  const matched = BY_KEYWORD.find((m) => m.kw.some((k) => name.includes(k)));
  if (matched) return matched.file;

  // id가 음수/0이어도 안전하게 0..N-1 인덱스로 매핑.
  const n = PLACEHOLDERS.length;
  const idx = ((p.id % n) + n) % n;
  return PLACEHOLDERS[idx];
}
