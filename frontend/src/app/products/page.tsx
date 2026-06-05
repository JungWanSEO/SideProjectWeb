"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { apiGet } from "@/lib/api";
import { PageResponse, Product } from "@/lib/types";

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

  if (loading) return <p className="p-8 text-gray-500">불러오는 중…</p>;
  if (error) return <p className="p-8 text-red-600">에러: {error}</p>;

  return (
    <main className="mx-auto max-w-5xl p-8">
      <h1 className="mb-6 text-2xl font-bold">상품 목록</h1>

      {products.length === 0 ? (
        <p className="text-gray-500">상품이 없습니다.</p>
      ) : (
        <ul className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {products.map((p) => (
            <li key={p.id}>
              <Link
                href={`/products/${p.id}`}
                className="block rounded-lg border border-gray-200 p-4 shadow-sm transition hover:shadow-md"
              >
              <div className="flex items-start justify-between gap-2">
                <h2 className="font-semibold">{p.name}</h2>
                {p.status === "SOLD_OUT" && (
                  <span className="shrink-0 rounded bg-gray-800 px-2 py-0.5 text-xs text-white">
                    품절
                  </span>
                )}
              </div>

              {p.brandName && <p className="text-sm text-gray-500">{p.brandName}</p>}

              <p className="mt-2 text-lg font-bold">{p.price.toLocaleString()}원</p>

              {/* 사이즈 옵션 — 품절 사이즈는 취소선 */}
              <div className="mt-3 flex flex-wrap gap-1">
                {p.options.map((o) => (
                  <span
                    key={o.id}
                    className={`rounded border px-2 py-0.5 text-xs ${
                      o.soldOut
                        ? "border-gray-200 text-gray-300 line-through"
                        : "border-gray-300 text-gray-600"
                    }`}
                  >
                    {o.size}
                  </span>
                ))}
              </div>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
