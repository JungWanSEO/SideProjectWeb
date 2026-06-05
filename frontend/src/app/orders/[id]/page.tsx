"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { Order } from "@/lib/types";
import { useAuth } from "@/lib/auth";

const STATUS_LABEL: Record<string, string> = { ORDERED: "주문완료", CANCELLED: "취소됨" };

/** 주문 상세 (/orders/[id]). 본인 주문만(서버가 403으로 차단). ORDERED면 취소 가능. */
export default function OrderDetailPage() {
  const params = useParams();
  const id = params.id as string;
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [order, setOrder] = useState<Order | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    if (!authLoading && !user) router.replace("/login");
  }, [authLoading, user, router]);

  useEffect(() => {
    if (!user) return;
    apiGet<Order>(`/api/orders/${id}`)
      .then(setOrder)
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user, id]);

  const cancel = async () => {
    if (!confirm("주문을 취소하시겠어요?")) return;
    setCancelling(true);
    setError(null);
    try {
      const updated = await apiPost<Order>(`/api/orders/${id}/cancel`);
      setOrder(updated); // 취소 API가 갱신된 주문(CANCELLED)을 돌려줌
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setCancelling(false);
    }
  };

  if (authLoading || (user && loading)) return <p className="p-8 text-gray-500">불러오는 중…</p>;
  if (!user) return null;

  if (error && !order) {
    return (
      <main className="mx-auto max-w-3xl p-8">
        <p className="text-red-600">에러: {error}</p>
        <Link href="/orders" className="mt-4 inline-block text-blue-600 hover:underline">
          ← 주문 내역
        </Link>
      </main>
    );
  }
  if (!order) return null;

  return (
    <main className="mx-auto max-w-3xl p-8">
      <Link href="/orders" className="text-sm text-gray-500 hover:underline">
        ← 주문 내역
      </Link>

      <div className="mt-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold">주문 #{order.id}</h1>
        <span
          className={`rounded px-2 py-1 text-xs ${
            order.status === "CANCELLED" ? "bg-gray-200 text-gray-500" : "bg-gray-900 text-white"
          }`}
        >
          {STATUS_LABEL[order.status] ?? order.status}
        </span>
      </div>
      <p className="mt-1 text-sm text-gray-400">{new Date(order.createdAt).toLocaleString("ko-KR")}</p>

      <ul className="mt-6 divide-y divide-gray-200 border-y border-gray-200">
        {order.items.map((it) => (
          <li key={it.optionId} className="flex items-center justify-between py-4">
            <div>
              <p className="font-medium">{it.productName}</p>
              <p className="text-sm text-gray-500">
                사이즈 {it.size} · {it.quantity}개 · {it.orderPrice.toLocaleString()}원
              </p>
            </div>
            <span className="font-semibold">{it.subtotal.toLocaleString()}원</span>
          </li>
        ))}
      </ul>

      <div className="mt-6 flex items-center justify-between">
        <span className="text-gray-500">합계</span>
        <span className="text-xl font-bold">{order.totalPrice.toLocaleString()}원</span>
      </div>

      {error && <p className="mt-4 text-sm text-red-600">{error}</p>}

      {order.status === "ORDERED" && (
        <button
          onClick={cancel}
          disabled={cancelling}
          className="mt-6 rounded border border-gray-300 px-4 py-2 text-sm text-gray-600 transition hover:border-red-400 hover:text-red-500 disabled:opacity-50"
        >
          {cancelling ? "취소 처리 중…" : "주문 취소"}
        </button>
      )}
    </main>
  );
}
