"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import { PageResponse, Settlement, SettlementRunResult } from "@/lib/types";
import { SETTLEMENT_STATUS_BADGE, SETTLEMENT_STATUS_LABEL } from "@/lib/settlementStatus";
import { PROVIDER_BADGE, formatRate, providerLabel } from "@/lib/provider";
import StatCard from "@/components/admin/StatCard";

/**
 * 정산(Settlement) 운영 화면 (/admin/settlements, ADMIN).
 * - 정산 배치 실행(PAID 결제 → 정산 항목) · 입금 처리(SCHEDULED → PAID_OUT)
 * - "매출 ≠ 결제액": gross / fee / net 분리 표시
 */
export default function AdminSettlementsPage() {
  const [items, setItems] = useState<Settlement[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [running, setRunning] = useState(false);
  const [runResult, setRunResult] = useState<SettlementRunResult | null>(null);
  const [payoutId, setPayoutId] = useState<number | null>(null);

  const load = useCallback(() => {
    setLoading(true);
    apiGet<PageResponse<Settlement>>("/api/settlements?size=50")
      .then((p) => setItems(p.content))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const runBatch = async () => {
    setRunning(true);
    setError(null);
    try {
      const r = await apiPost<SettlementRunResult>("/api/settlements/run");
      setRunResult(r);
      load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  const payout = async (id: number) => {
    setPayoutId(id);
    setError(null);
    try {
      await apiPost<Settlement>(`/api/settlements/${id}/payout`);
      load();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setPayoutId(null);
    }
  };

  // 현재 로드된 정산 합계 (KPI 카드)
  const totals = items.reduce(
    (acc, s) => ({ gross: acc.gross + s.grossAmount, fee: acc.fee + s.fee, net: acc.net + s.netAmount }),
    { gross: 0, fee: 0, net: 0 },
  );

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">정산</h1>
          <p className="text-sm text-gray-500">PAID 결제를 정산 항목으로 만들고 입금까지 추적합니다.</p>
        </div>
        <button
          onClick={runBatch}
          disabled={running}
          className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
        >
          {running ? "실행 중…" : "정산 배치 실행"}
        </button>
      </div>

      {runResult && (
        <div className="mb-4 rounded border border-green-200 bg-green-50 px-4 py-3 text-sm text-green-800">
          정산 배치 완료 — 신규 <b>{runResult.createdCount}</b>건 · 결제액{" "}
          {runResult.totalGrossAmount.toLocaleString()}원 · 수수료 {runResult.totalFee.toLocaleString()}원 · 실입금{" "}
          {runResult.totalNetAmount.toLocaleString()}원
          {/* PG별 분해 — 같은 금액도 PG 요율에 따라 수수료가 갈린다(MPG-3) */}
          {runResult.byProvider.length > 0 && (
            <div className="mt-2 flex flex-wrap gap-2">
              {runResult.byProvider.map((b) => (
                <span key={b.provider} className="rounded border border-green-200 bg-white px-2 py-1 text-xs text-gray-600">
                  <span className={`rounded px-1.5 py-0.5 ${PROVIDER_BADGE}`}>{providerLabel(b.provider)}</span>{" "}
                  {formatRate(b.feeRate)} · {b.count}건 · 수수료 {b.fee.toLocaleString()}원 · 실입금{" "}
                  {b.netAmount.toLocaleString()}원
                </span>
              ))}
            </div>
          )}
        </div>
      )}
      {error && (
        <div className="mb-4 rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {/* KPI 카드 — 매출 ≠ 결제액을 한눈에 */}
      <div className="mb-6 grid grid-cols-4 gap-4">
        <StatCard label="정산 건수" value={`${items.length}건`} />
        <StatCard label="결제액 (gross)" value={`${totals.gross.toLocaleString()}원`} />
        <StatCard label="수수료 (fee)" value={`−${totals.fee.toLocaleString()}원`} accent="text-amber-600" />
        <StatCard label="실입금 (net)" value={`${totals.net.toLocaleString()}원`} accent="text-green-700" />
      </div>

      {/* 정산 테이블 */}
      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
            <tr>
              <th className="px-4 py-3">ID</th>
              <th className="px-4 py-3">PG</th>
              <th className="px-4 py-3">거래 ID</th>
              <th className="px-4 py-3 text-right">결제액</th>
              <th className="px-4 py-3 text-right">수수료</th>
              <th className="px-4 py-3 text-right">요율</th>
              <th className="px-4 py-3 text-right">실입금</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3">입금예정일</th>
              <th className="px-4 py-3 text-right">액션</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr>
                <td colSpan={10} className="px-4 py-8 text-center text-gray-400">
                  불러오는 중…
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={10} className="px-4 py-8 text-center text-gray-400">
                  정산 항목이 없습니다. “정산 배치 실행”을 눌러보세요.
                </td>
              </tr>
            ) : (
              items.map((s) => (
                <tr key={s.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3 text-gray-400">#{s.id}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs ${PROVIDER_BADGE}`}>{providerLabel(s.provider)}</span>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{s.pgTransactionId.slice(0, 16)}…</td>
                  <td className="px-4 py-3 text-right">{s.grossAmount.toLocaleString()}</td>
                  <td className="px-4 py-3 text-right text-amber-600">−{s.fee.toLocaleString()}</td>
                  <td className="px-4 py-3 text-right text-gray-500">{formatRate(s.feeRate)}</td>
                  <td className="px-4 py-3 text-right font-medium">{s.netAmount.toLocaleString()}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs ${SETTLEMENT_STATUS_BADGE[s.status]}`}>
                      {SETTLEMENT_STATUS_LABEL[s.status]}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-gray-500">{s.settledDate}</td>
                  <td className="px-4 py-3 text-right">
                    {s.status === "SCHEDULED" ? (
                      <button
                        onClick={() => payout(s.id)}
                        disabled={payoutId === s.id}
                        className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-100 disabled:opacity-50"
                      >
                        {payoutId === s.id ? "처리 중…" : "입금 처리"}
                      </button>
                    ) : (
                      <span className="text-xs text-gray-300">—</span>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
