"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { Cart, Product } from "@/lib/types";
import { useAuth } from "@/lib/auth";

/**
 * 상품 상세 페이지 (/products/[id]).
 * 사이즈(옵션)를 고르고 장바구니에 담는다. 담기는 인증 필요 → 비로그인 시 로그인 페이지로.
 */
export default function ProductDetailPage() {
  const params = useParams();
  const id = params.id as string; // 동적 세그먼트 [id]
  const router = useRouter();
  const { user } = useAuth();

  const [product, setProduct] = useState<Product | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [selectedOptionId, setSelectedOptionId] = useState<number | null>(null);
  const [adding, setAdding] = useState(false);
  const [cartMsg, setCartMsg] = useState<string | null>(null);

  useEffect(() => {
    apiGet<Product>(`/api/products/${id}`)
      .then(setProduct)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [id]);

  const addToCart = async () => {
    if (!user) {
      router.push("/login"); // 비로그인 → 로그인으로
      return;
    }
    if (selectedOptionId === null) {
      setCartMsg("사이즈를 선택하세요.");
      return;
    }
    setAdding(true);
    setCartMsg(null);
    try {
      await apiPost<Cart>("/api/carts/items", { optionId: selectedOptionId, quantity: 1 });
      setCartMsg("장바구니에 담았습니다.");
    } catch (e) {
      setCartMsg((e as Error).message);
    } finally {
      setAdding(false);
    }
  };

  if (loading) return <p className="p-8 text-gray-500">불러오는 중…</p>;

  if (error) {
    return (
      <main className="mx-auto max-w-3xl p-8">
        <p className="text-red-600">에러: {error}</p>
        <Link href="/products" className="mt-4 inline-block text-blue-600 hover:underline">
          ← 목록으로
        </Link>
      </main>
    );
  }

  if (!product) return null;

  return (
    <main className="mx-auto max-w-3xl p-8">
      <Link href="/products" className="text-sm text-gray-500 hover:underline">
        ← 목록으로
      </Link>

      <div className="mt-4 flex items-start justify-between gap-3">
        <div>
          {product.brandName && <p className="text-sm text-gray-500">{product.brandName}</p>}
          <h1 className="text-2xl font-bold">{product.name}</h1>
          {product.categoryName && (
            <p className="mt-1 text-sm text-gray-400">{product.categoryName}</p>
          )}
        </div>
        {product.status === "SOLD_OUT" && (
          <span className="shrink-0 rounded bg-gray-800 px-2 py-1 text-xs text-white">품절</span>
        )}
      </div>

      <p className="mt-4 text-2xl font-bold">{product.price.toLocaleString()}원</p>

      {product.description && (
        <p className="mt-4 whitespace-pre-line text-gray-700">{product.description}</p>
      )}

      {/* 사이즈 선택: 품절은 비활성, 선택된 사이즈는 강조 */}
      <h2 className="mb-2 mt-8 font-semibold">사이즈</h2>
      <div className="flex flex-wrap gap-2">
        {product.options.map((o) => {
          const selected = o.id === selectedOptionId;
          return (
            <button
              key={o.id}
              type="button"
              disabled={o.soldOut}
              onClick={() => setSelectedOptionId(o.id)}
              className={`rounded-lg border px-4 py-2 text-sm transition ${
                o.soldOut
                  ? "cursor-not-allowed border-gray-200 text-gray-300"
                  : selected
                    ? "border-gray-900 bg-gray-900 text-white"
                    : "border-gray-300 text-gray-700 hover:border-gray-900"
              }`}
            >
              <span className={o.soldOut ? "line-through" : ""}>{o.size}</span>
              <span className={`ml-2 text-xs ${selected ? "text-gray-300" : "text-gray-400"}`}>
                {o.soldOut ? "품절" : `재고 ${o.stock}`}
              </span>
            </button>
          );
        })}
      </div>

      {/* 담기 */}
      <div className="mt-6 flex items-center gap-3">
        <button
          type="button"
          onClick={addToCart}
          disabled={adding}
          className="rounded bg-gray-900 px-6 py-3 text-white transition hover:bg-gray-700 disabled:opacity-50"
        >
          {adding ? "담는 중…" : "장바구니 담기"}
        </button>
        {cartMsg && <span className="text-sm text-gray-600">{cartMsg}</span>}
      </div>
      {!user && (
        <p className="mt-2 text-xs text-gray-400">담으려면 로그인이 필요합니다.</p>
      )}
    </main>
  );
}
