"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { apiGet } from "@/lib/api";
import { PageResponse, Product } from "@/lib/types";
import ProductThumb from "@/components/ui/ProductThumb";
import Badge from "@/components/ui/Badge";

/**
 * 상품 목록 페이지 (/products).
 * 클라이언트 컴포넌트에서 GET /api/products 를 호출 → 브라우저가 직접 백엔드를 부른다(CORS 필요).
 */
export default function ProductsPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<PageResponse<Product>>("/api/products")
      .then((page) => setProducts(page.content))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <p className="p-12 text-center text-muted">불러오는 중…</p>;
  if (error) return <p className="p-12 text-center text-danger">에러: {error}</p>;

  return (
    <main className="mx-auto max-w-6xl px-6 py-12">
      <header className="mb-10">
        <span className="text-xs uppercase tracking-[0.3em] text-clay">Collection</span>
        <h1 className="mt-2 text-3xl font-bold text-ink">전체 상품</h1>
      </header>

      {products.length === 0 ? (
        <p className="text-muted">상품이 없습니다.</p>
      ) : (
        <ul className="grid grid-cols-2 gap-x-5 gap-y-10 lg:grid-cols-3">
          {products.map((p) => (
            <li key={p.id}>
              <Link href={`/products/${p.id}`} className="group block">
                <div className="relative overflow-hidden rounded-2xl">
                  <ProductThumb
                    name={p.name}
                    className="aspect-[4/5] w-full transition duration-500 group-hover:scale-[1.03]"
                  />
                  {p.status === "SOLD_OUT" && (
                    <span className="absolute left-3 top-3">
                      <Badge tone="dark">품절</Badge>
                    </span>
                  )}
                </div>

                <div className="mt-3 px-0.5">
                  {p.brandName && (
                    <p className="text-xs uppercase tracking-wider text-muted">{p.brandName}</p>
                  )}
                  <h2 className="mt-1 font-serif text-lg text-ink">{p.name}</h2>
                  <p className="mt-1 font-medium text-ink">{p.price.toLocaleString()}원</p>

                  {/* 사이즈 옵션 — 품절 사이즈는 흐리게 취소선 */}
                  <div className="mt-2 flex flex-wrap gap-1">
                    {p.options.map((o) => (
                      <span
                        key={o.id}
                        className={`rounded-full border px-2 py-0.5 text-[11px] ${
                          o.soldOut
                            ? "border-line text-line line-through"
                            : "border-line text-muted"
                        }`}
                      >
                        {o.size}
                      </span>
                    ))}
                  </div>
                </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
