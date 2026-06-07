"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet, apiDelete, apiPost } from "@/lib/api";
import { Cart, Order } from "@/lib/types";
import { useAuth } from "@/lib/auth";

/**
 * 장바구니 페이지 (/cart). 인증 필요(httpOnly 쿠키 자동 전송).
 * 비로그인 시 로그인 페이지로 보낸다.
 */
export default function CartPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [cart, setCart] = useState<Cart | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [ordering, setOrdering] = useState(false);

  // 비로그인 → 로그인으로
  useEffect(() => {
    if (!authLoading && !user) router.replace("/login");
  }, [authLoading, user, router]);

  useEffect(() => {
    if (!user) return;
    apiGet<Cart>("/api/carts")
      .then(setCart)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user]);

  const remove = async (optionId: number) => {
    try {
      const updated = await apiDelete<Cart>(`/api/carts/items/${optionId}`);
      setCart(updated); // 삭제 API가 갱신된 장바구니를 돌려줌
    } catch (e) {
      setError((e as Error).message);
    }
  };

  // 체크아웃: 서버가 장바구니 → 주문(PENDING) 변환 + 장바구니 비우기 (한 번의 호출).
  // 주문은 아직 '결제 대기'일 뿐 → 곧장 결제 화면으로 보낸다(재고 차감은 결제 승인 시점).
  const checkout = async () => {
    setOrdering(true);
    setError(null);
    try {
      const order = await apiPost<Order>("/api/orders/checkout");
      router.push(`/orders/${order.id}/pay`); // 결제 화면으로 이동 (이 컴포넌트는 언마운트)
    } catch (e) {
      setError((e as Error).message); // 재고부족(409)·빈 장바구니(400) 등
      setOrdering(false);
    }
  };

  if (authLoading || (user && loading)) return <p className="p-8 text-gray-500">불러오는 중…</p>;
  if (!user) return null; // 리다이렉트 중

  const items = cart?.items ?? [];
  const totalPrice = items.reduce((sum, it) => sum + it.subtotal, 0);

  return (
    <main className="mx-auto max-w-3xl p-8">
      <h1 className="mb-6 text-2xl font-bold">장바구니</h1>

      {items.length === 0 ? (
        <div className="text-gray-500">
          <p>장바구니가 비어 있습니다.</p>
          <Link href="/products" className="mt-2 inline-block text-blue-600 hover:underline">
            상품 보러가기 →
          </Link>
        </div>
      ) : (
        <>
          <ul className="divide-y divide-gray-200 border-y border-gray-200">
            {items.map((it) => (
              <li key={it.optionId} className="flex items-center justify-between gap-4 py-4">
                <div>
                  <p className="font-medium">{it.productName}</p>
                  <p className="text-sm text-gray-500">
                    사이즈 {it.size} · {it.quantity}개
                    {it.soldOut && <span className="ml-2 text-red-500">품절</span>}
                  </p>
                </div>
                <div className="flex items-center gap-4">
                  <span className="font-semibold">{it.subtotal.toLocaleString()}원</span>
                  <button
                    onClick={() => remove(it.optionId)}
                    className="text-sm text-gray-400 hover:text-red-500 hover:underline"
                  >
                    삭제
                  </button>
                </div>
              </li>
            ))}
          </ul>

          <div className="mt-6 flex items-center justify-between">
            <span className="text-gray-500">총 {cart?.totalQuantity ?? 0}개</span>
            <span className="text-xl font-bold">{totalPrice.toLocaleString()}원</span>
          </div>

          {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

          <button
            onClick={checkout}
            disabled={ordering}
            className="mt-4 w-full rounded bg-gray-900 px-4 py-3 text-white transition hover:bg-gray-700 disabled:opacity-50"
          >
            {ordering ? "주문 처리 중…" : "주문하기"}
          </button>
        </>
      )}
    </main>
  );
}
