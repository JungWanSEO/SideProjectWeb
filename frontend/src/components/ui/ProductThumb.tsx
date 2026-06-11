// 상품 이미지 placeholder — 더미 데이터에 이미지가 없어, 이름 해시로 따뜻한 그라데이션 +
// 머리글자(세리프)를 그려 비주얼을 채운다. (실제 이미지 필드가 생기면 <img>로 교체)
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

export default function ProductThumb({ name, className = "" }: { name: string; className?: string }) {
  const gradient = GRADIENTS[hash(name) % GRADIENTS.length];
  // "P01-Cap" → "Cap" → "C". 이름이 비슷해도 머리글자가 다양해지도록 대시 뒷부분을 쓴다.
  const label = name.includes("-") ? name.slice(name.lastIndexOf("-") + 1) : name;
  const initial = (label.trim()[0] ?? "·").toUpperCase();
  return (
    <div className={`flex items-center justify-center bg-gradient-to-br ${gradient} ${className}`}>
      <span className="select-none font-serif text-4xl text-ink/25">{initial}</span>
    </div>
  );
}
