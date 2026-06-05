"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { apiGet } from "@/lib/api";
import { OrderSummary, PageResponse } from "@/lib/types";
import { useAuth } from "@/lib/auth";

const STATUS_LABEL: Record<string, string> = { ORDERED: "주문완료", CANCELLED: "취소됨" };

/** 내 주문 목록 (/orders). 인증 필요. 목록은 요약(대표상품명+항목수). */
export default function OrdersPage() {
  const { user, loading: authLoading } = useAuth();
  const router = useRouter();

  const [orders, setOrders] = useState<OrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!authLoading && !user) router.replace("/login");
  }, [authLoading, user, router]);

  useEffect(() => {
    if (!user) return;
    apiGet<PageResponse<OrderSummary>>("/api/orders")
      .then((page) => setOrders(page.content))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [user]);

  if (authLoading || (user && loading)) return <p className="p-8 text-gray-500">불러오는 중…</p>;
  if (!user) return null;
  if (error) return <p className="p-8 text-red-600">에러: {error}</p>;

  return (
    <main className="mx-auto max-w-3xl p-8">
      <h1 className="mb-6 text-2xl font-bold">주문 내역</h1>

      {orders.length === 0 ? (
        <div className="text-gray-500">
          <p>주문 내역이 없습니다.</p>
          <Link href="/products" className="mt-2 inline-block text-blue-600 hover:underline">
            상품 보러가기 →
          </Link>
        </div>
      ) : (
        <ul className="flex flex-col gap-3">
          {orders.map((o) => (
            <li key={o.id}>
              <Link
                href={`/orders/${o.id}`}
                className="block rounded-lg border border-gray-200 p-4 transition hover:shadow-md"
              >
                <div className="flex items-center justify-between">
                  <span className="text-sm text-gray-400">
                    #{o.id} · {new Date(o.createdAt).toLocaleDateString("ko-KR")}
                  </span>
                  <span
                    className={`rounded px-2 py-0.5 text-xs ${
                      o.status === "CANCELLED" ? "bg-gray-200 text-gray-500" : "bg-gray-900 text-white"
                    }`}
                  >
                    {STATUS_LABEL[o.status] ?? o.status}
                  </span>
                </div>
                <p className="mt-2 font-medium">
                  {o.representativeProductName}
                  {o.itemCount > 1 && <span className="text-gray-500"> 외 {o.itemCount - 1}건</span>}
                </p>
                <p className="mt-1 font-bold">{o.totalPrice.toLocaleString()}원</p>
              </Link>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}
