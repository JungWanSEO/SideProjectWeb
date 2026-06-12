"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { Cart, Address, Order } from "@/lib/types";
import { useAuth } from "@/lib/auth";
import Badge from "@/components/ui/Badge";

/**
 * 주문서 / 체크아웃 확인 페이지 (/checkout). 장바구니 → 결제 사이 단계.
 * 주문 요약 + 배송지 선택(주소록) + 배송 메모 → POST /api/orders/checkout(addressId) → 결제 화면.
 * 항목은 서버 장바구니가 진실의 원천이라 보내지 않는다(여기선 표시만).
 */
export default function CheckoutPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [cart, setCart] = useState<Cart | null>(null);
  const [addresses, setAddresses] = useState<Address[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [memo, setMemo] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [ordering, setOrdering] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) router.replace("/login");
  }, [authLoading, user, router]);

  useEffect(() => {
    if (!user) return;
    // 장바구니 + 주소록을 함께 로드. 기본배송지(없으면 첫 주소)를 미리 선택.
    Promise.all([apiGet<Cart>("/api/carts"), apiGet<Address[]>("/api/addresses")])
      .then(([c, a]) => {
        setCart(c);
        setAddresses(a);
        const def = a.find((x) => x.isDefault) ?? a[0];
        setSelectedId(def ? def.id : null);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user]);

  const placeOrder = async () => {
    if (selectedId == null) {
      setError("배송지를 선택해 주세요.");
      return;
    }
    setOrdering(true);
    setError(null);
    try {
      const order = await apiPost<Order>("/api/orders/checkout", {
        addressId: selectedId,
        deliveryMemo: memo.trim() || null,
      });
      router.push(`/orders/${order.id}/pay`); // 주문(PENDING) 생성됨 → 결제 화면으로
    } catch (e) {
      setError((e as Error).message); // 빈 장바구니(400)·재고 등
      setOrdering(false);
    }
  };

  if (authLoading || (user && loading))
    return <p className="p-12 text-center text-muted">불러오는 중…</p>;
  if (!user) return null;

  const items = cart?.items ?? [];
  const total = items.reduce((s, it) => s + it.subtotal, 0);

  if (items.length === 0) {
    return (
      <main className="mx-auto max-w-3xl px-6 py-12">
        <h1 className="mb-6 text-3xl font-bold text-ink">주문서</h1>
        <div className="rounded-2xl border border-line bg-paper p-12 text-center">
          <p className="text-muted">장바구니가 비어 있습니다.</p>
          <Link href="/products" className="mt-3 inline-block text-clay hover:underline">
            상품 보러가기 →
          </Link>
        </div>
      </main>
    );
  }

  return (
    <main className="mx-auto max-w-3xl px-6 py-12">
      <h1 className="mb-8 text-3xl font-bold text-ink">주문서</h1>

      {/* 배송지 선택 */}
      <section className="mb-8">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="font-serif text-xl text-ink">배송지</h2>
          <Link href="/account/addresses" className="text-sm text-muted transition hover:text-clay">
            주소 관리 →
          </Link>
        </div>

        {addresses.length === 0 ? (
          <div className="rounded-2xl border border-line bg-paper p-6 text-center text-sm text-muted">
            저장된 배송지가 없습니다.{" "}
            <Link href="/account/addresses" className="text-clay hover:underline">
              배송지를 먼저 등록
            </Link>
            해 주세요.
          </div>
        ) : (
          <ul className="flex flex-col gap-2">
            {addresses.map((a) => (
              <li key={a.id}>
                <label
                  className={`flex cursor-pointer items-start gap-3 rounded-2xl border p-4 transition ${
                    selectedId === a.id
                      ? "border-clay bg-clay-50/40"
                      : "border-line bg-paper hover:border-clay/50"
                  }`}
                >
                  <input
                    type="radio"
                    name="address"
                    className="mt-1 accent-clay"
                    checked={selectedId === a.id}
                    onChange={() => setSelectedId(a.id)}
                  />
                  <div className="min-w-0">
                    <p className="flex items-center gap-2 font-medium text-ink">
                      {a.recipient}
                      {a.isDefault && <Badge tone="sage">기본배송지</Badge>}
                    </p>
                    <p className="mt-0.5 text-sm text-muted">{a.phone}</p>
                    <p className="mt-0.5 text-sm text-ink/80">
                      [{a.zipcode}] {a.address1}
                      {a.address2 ? ` ${a.address2}` : ""}
                    </p>
                  </div>
                </label>
              </li>
            ))}
          </ul>
        )}

        <input
          className="mt-3 w-full rounded-xl border border-line bg-paper px-4 py-2.5 text-ink outline-none transition placeholder:text-muted focus:border-clay"
          placeholder="배송 요청사항 (선택)"
          maxLength={200}
          value={memo}
          onChange={(e) => setMemo(e.target.value)}
        />
      </section>

      {/* 주문 상품 요약 */}
      <section className="mb-8">
        <h2 className="mb-3 font-serif text-xl text-ink">주문 상품</h2>
        <ul className="overflow-hidden rounded-2xl border border-line bg-paper">
          {items.map((it, i) => (
            <li
              key={it.optionId}
              className={`flex items-center justify-between px-5 py-4 ${i > 0 ? "border-t border-line" : ""}`}
            >
              <div>
                <p className="text-ink">{it.productName}</p>
                <p className="mt-0.5 text-sm text-muted">
                  사이즈 {it.size} · {it.quantity}개
                </p>
              </div>
              <span className="font-medium text-ink">{it.subtotal.toLocaleString()}원</span>
            </li>
          ))}
        </ul>
      </section>

      <div className="flex items-center justify-between border-t border-line pt-5">
        <span className="text-muted">총 {cart?.totalQuantity ?? 0}개</span>
        <span className="text-2xl font-bold text-ink">{total.toLocaleString()}원</span>
      </div>

      {error && <p className="mt-4 text-sm text-danger">{error}</p>}

      <button
        onClick={placeOrder}
        disabled={ordering || addresses.length === 0}
        className="mt-6 w-full rounded-full bg-clay px-4 py-3.5 font-medium text-cream transition hover:bg-clay-600 disabled:opacity-50"
      >
        {ordering ? "주문 처리 중…" : "주문하고 결제하기"}
      </button>
    </main>
  );
}
