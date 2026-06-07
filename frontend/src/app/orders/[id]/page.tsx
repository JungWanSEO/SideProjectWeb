"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet, apiPost } from "@/lib/api";
import { Order } from "@/lib/types";
import { ORDER_STATUS_LABEL, ORDER_STATUS_BADGE } from "@/lib/orderStatus";
import { useAuth } from "@/lib/auth";

/** 주문 상세 (/orders/[id]). 본인 주문만(서버가 403으로 차단). PENDING=결제/취소, PAID=취소(환불). */
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
    // PAID 주문 취소는 환불까지 동반 → 문구로 구분
    const msg = order?.status === "PAID" ? "결제를 취소하고 환불하시겠어요?" : "주문을 취소하시겠어요?";
    if (!confirm(msg)) return;
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
        <span className={`rounded px-2 py-1 text-xs ${ORDER_STATUS_BADGE[order.status]}`}>
          {ORDER_STATUS_LABEL[order.status]}
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

      <div className="mt-6 flex gap-3">
        {/* PENDING(결제대기): 결제하기 + 취소 / PAID(결제완료): 취소=환불 / CANCELLED: 버튼 없음 */}
        {order.status === "PENDING" && (
          <Link
            href={`/orders/${order.id}/pay`}
            className="rounded bg-gray-900 px-4 py-2 text-sm text-white transition hover:bg-gray-700"
          >
            결제하기
          </Link>
        )}
        {(order.status === "PENDING" || order.status === "PAID") && (
          <button
            onClick={cancel}
            disabled={cancelling}
            className="rounded border border-gray-300 px-4 py-2 text-sm text-gray-600 transition hover:border-red-400 hover:text-red-500 disabled:opacity-50"
          >
            {cancelling ? "취소 처리 중…" : "주문 취소"}
          </button>
        )}
      </div>
    </main>
  );
}
