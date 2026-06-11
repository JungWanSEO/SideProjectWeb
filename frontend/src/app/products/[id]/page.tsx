"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { Cart, Product } from "@/lib/types";
import { useAuth } from "@/lib/auth";
import ProductThumb from "@/components/ui/ProductThumb";
import Badge from "@/components/ui/Badge";
import { buttonClass } from "@/components/ui/Button";

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

  if (loading) return <p className="p-12 text-center text-muted">불러오는 중…</p>;

  if (error) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-12">
        <p className="text-danger">에러: {error}</p>
        <Link href="/products" className="mt-4 inline-block text-clay hover:underline">
          ← 목록으로
        </Link>
      </main>
    );
  }

  if (!product) return null;

  return (
    <main className="mx-auto max-w-6xl px-6 py-10">
      <Link href="/products" className="text-sm text-muted transition hover:text-clay">
        ← 목록으로
      </Link>

      <div className="mt-6 grid gap-10 lg:grid-cols-2">
        {/* 이미지 */}
        <div className="relative">
          <ProductThumb name={product.name} className="aspect-[4/5] w-full rounded-2xl shadow-soft" />
          {product.status === "SOLD_OUT" && (
            <span className="absolute left-4 top-4">
              <Badge tone="dark">품절</Badge>
            </span>
          )}
        </div>

        {/* 정보 */}
        <div className="lg:py-4">
          {product.brandName && (
            <p className="text-xs uppercase tracking-[0.25em] text-clay">{product.brandName}</p>
          )}
          <h1 className="mt-2 font-serif text-3xl text-ink">{product.name}</h1>
          {product.categoryName && <p className="mt-1 text-sm text-muted">{product.categoryName}</p>}

          <p className="mt-5 text-2xl font-semibold text-ink">{product.price.toLocaleString()}원</p>

          {product.description && (
            <p className="mt-5 whitespace-pre-line leading-relaxed text-ink/80">{product.description}</p>
          )}

          {/* 사이즈 선택: 품절은 비활성, 선택된 사이즈는 점토색 강조 */}
          <h2 className="mb-2 mt-8 text-sm font-medium text-muted">사이즈</h2>
          <div className="flex flex-wrap gap-2">
            {product.options.map((o) => {
              const selected = o.id === selectedOptionId;
              return (
                <button
                  key={o.id}
                  type="button"
                  disabled={o.soldOut}
                  onClick={() => setSelectedOptionId(o.id)}
                  className={`rounded-full border px-4 py-2 text-sm transition ${
                    o.soldOut
                      ? "cursor-not-allowed border-line text-line"
                      : selected
                        ? "border-clay bg-clay text-cream"
                        : "border-line text-ink hover:border-clay"
                  }`}
                >
                  <span className={o.soldOut ? "line-through" : ""}>{o.size}</span>
                  <span className={`ml-2 text-xs ${selected ? "text-cream/70" : "text-muted"}`}>
                    {o.soldOut ? "품절" : `재고 ${o.stock}`}
                  </span>
                </button>
              );
            })}
          </div>

          {/* 담기 */}
          <div className="mt-8 flex items-center gap-3">
            <button
              type="button"
              onClick={addToCart}
              disabled={adding}
              className={buttonClass("primary", "lg")}
            >
              {adding ? "담는 중…" : "장바구니 담기"}
            </button>
            {cartMsg && <span className="text-sm text-muted">{cartMsg}</span>}
          </div>
          {!user && <p className="mt-2 text-xs text-muted">담으려면 로그인이 필요합니다.</p>}
        </div>
      </div>
    </main>
  );
}
