"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { Order, Payment } from "@/lib/types";
import { PROVIDERS } from "@/lib/provider";
import { useAuth } from "@/lib/auth";

// 모의 결제수단 (백엔드는 method 문자열만 받음 — 기본 MOCK_CARD)
const METHODS = [
  { value: "MOCK_CARD", label: "신용/체크카드 (모의)" },
  { value: "MOCK_BANK", label: "계좌이체 (모의)" },
];

// 결제 PG 선택지 — "자동"은 서버가 가장 싼 PG로 라우팅(비용기반). 나머지는 명시 선택.
const PG_OPTIONS = [{ value: "AUTO", label: "자동 (최저 수수료)" }, ...PROVIDERS];

/**
 * 결제 화면 (/orders/[id]/pay). PENDING 주문만 결제 가능.
 * 흐름: 주문 조회 → (PENDING 아니면 상세로 되돌림) → 결제수단 선택 → POST /api/payments → 상세(PAID).
 */
export default function PaymentPage() {
  const params = useParams();
  const id = params.id as string;
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [method, setMethod] = useState("MOCK_CARD");
  const [provider, setProvider] = useState<string>("TOSS"); // 결제 PG — 라우터가 장애 시 다른 PG로 페일오버
  const [paying, setPaying] = useState(false);

  // 멱등키: 이 결제 화면 진입당 1개 고정 → 더블클릭/네트워크 재시도해도 같은 키 = 중복결제 방지(백엔드 멱등성과 짝).
  // crypto.randomUUID()를 useEffect(클라이언트)에서 생성 — SSR/클라 값이 달라 hydration 불일치 나는 걸 피함.
  const [idempotencyKey, setIdempotencyKey] = useState<string | null>(null);
  useEffect(() => {
    setIdempotencyKey(crypto.randomUUID());
  }, []);

  useEffect(() => {
    if (!authLoading && !user) router.replace("/login");
  }, [authLoading, user, router]);

  useEffect(() => {
    if (!user) return;
    apiGet<Order>(`/api/orders/${id}`)
      .then((o) => {
        setOrder(o);
        // 이미 결제됐거나(PAID) 취소된(CANCELLED) 주문이면 결제 화면이 무의미 → 상세로
        if (o.status !== "PENDING") router.replace(`/orders/${id}`);
      })
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, id, router]);

  const pay = async () => {
    if (!idempotencyKey) return; // 키 준비 전(마운트 직후 찰나) 클릭 방어
    setPaying(true);
    setError(null);
    try {
      await apiPost<Payment>("/api/payments", { orderId: Number(id), idempotencyKey, method, provider });
      router.push(`/orders/${id}`); // 결제 완료 → 주문 상세(PAID)
    } catch (e) {
      setError((e as Error).message); // PG 거절(402) / 결제 불가 상태(409) / 재고부족 등
      setPaying(false);
    }
  };

  if (authLoading || (user && loading)) return <p className="p-8 text-gray-500">불러오는 중…</p>;
  if (!user) return null;

  if (error && !order) {
    return (
      <main className="mx-auto max-w-md p-8">
        <p className="text-red-600">에러: {error}</p>
        <Link href="/orders" className="mt-4 inline-block text-blue-600 hover:underline">← 주문 내역</Link>
      </main>
    );
  }
  if (!order || order.status !== "PENDING") return null; // 상세로 리다이렉트 중

  return (
    <main className="mx-auto max-w-md p-8">
      <Link href={`/orders/${id}`} className="text-sm text-gray-500 hover:underline">← 주문 상세</Link>
      <h1 className="mt-4 text-2xl font-bold">결제</h1>
      <p className="mt-1 text-sm text-gray-400">주문 #{order.id}</p>

      {/* 주문 요약 */}
      <ul className="mt-6 divide-y divide-gray-200 border-y border-gray-200">
        {order.items.map((it) => (
          <li key={it.optionId} className="flex items-center justify-between py-3 text-sm">
            <span className="text-gray-700">
              {it.productName} · {it.size} · {it.quantity}개
            </span>
            <span className="font-medium">{it.subtotal.toLocaleString()}원</span>
          </li>
        ))}
      </ul>
      <div className="mt-4 flex items-center justify-between">
        <span className="text-gray-500">결제 금액</span>
        <span className="text-xl font-bold">{order.totalPrice.toLocaleString()}원</span>
      </div>

      {/* 결제 PG (다중 PG) — 고른 PG로 승인하고, 장애 시 서버가 다른 PG로 자동 페일오버 */}
      <fieldset className="mt-6">
        <legend className="mb-2 text-sm font-medium text-gray-600">결제 PG</legend>
        <div className="grid grid-cols-3 gap-2">
          {PG_OPTIONS.map((pg) => (
            <label
              key={pg.value}
              className={`flex cursor-pointer items-center justify-center gap-2 rounded border p-3 text-sm transition ${
                provider === pg.value ? "border-gray-900 bg-gray-50 font-medium" : "border-gray-200 hover:border-gray-400"
              }`}
            >
              <input
                type="radio"
                name="provider"
                value={pg.value}
                checked={provider === pg.value}
                onChange={() => setProvider(pg.value)}
                className="sr-only"
              />
              {pg.label}
            </label>
          ))}
        </div>
      </fieldset>

      {/* 결제수단 */}
      <fieldset className="mt-6">
        <legend className="mb-2 text-sm font-medium text-gray-600">결제수단</legend>
        <div className="flex flex-col gap-2">
          {METHODS.map((m) => (
            <label
              key={m.value}
              className="flex cursor-pointer items-center gap-2 rounded border border-gray-200 p-3 text-sm hover:border-gray-400"
            >
              <input
                type="radio"
                name="method"
                value={m.value}
                checked={method === m.value}
                onChange={() => setMethod(m.value)}
              />
              {m.label}
            </label>
          ))}
        </div>
      </fieldset>

      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

      <button
        onClick={pay}
        disabled={paying || !idempotencyKey}
        className="mt-6 w-full rounded bg-gray-900 px-4 py-3 text-white transition hover:bg-gray-700 disabled:opacity-50"
      >
        {paying ? "결제 처리 중…" : `${order.totalPrice.toLocaleString()}원 결제하기`}
      </button>
      <p className="mt-3 text-center text-xs text-gray-400">모의 결제 — 실제로 청구되지 않습니다.</p>
    </main>
  );
}
