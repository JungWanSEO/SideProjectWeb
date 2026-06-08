"use client";

import { useCallback, useEffect, useState } from "react";
import { apiGet, apiPost } from "@/lib/api";
import { Mismatch, MismatchStatus, PageResponse, ReconciliationResult } from "@/lib/types";
import {
  MISMATCH_STATUS_BADGE,
  MISMATCH_STATUS_LABEL,
  MISMATCH_TYPE_BADGE,
  MISMATCH_TYPE_LABEL,
} from "@/lib/mismatchStatus";
import StatCard from "@/components/admin/StatCard";

type Tab = MismatchStatus | "ALL";
const TABS: { key: Tab; label: string }[] = [
  { key: "OPEN", label: "미처리" },
  { key: "RESOLVED", label: "처리됨" },
  { key: "IGNORED", label: "무시" },
  { key: "ALL", label: "전체" },
];

const won = (n: number | null) => (n === null ? "—" : `${n.toLocaleString()}원`);

/**
 * 대사(Reconciliation) 운영 화면 (/admin/reconciliations, ADMIN).
 * - 대사 실행(우리 정산 ↔ PG 리포트 대조) · 불일치 처리(resolve/ignore)
 * - 처리한 불일치는 재대사에서 다시 열리지 않는다(alreadyHandled).
 */
export default function AdminReconciliationsPage() {
  const [items, setItems] = useState<Mismatch[]>([]);
  const [tab, setTab] = useState<Tab>("OPEN");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<ReconciliationResult | null>(null);
  const [actingId, setActingId] = useState<number | null>(null);

  const load = useCallback((status: Tab) => {
    setLoading(true);
    const q = status === "ALL" ? "?size=50" : `?status=${status}&size=50`;
    apiGet<PageResponse<Mismatch>>(`/api/reconciliations/mismatches${q}`)
      .then((p) => setItems(p.content))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load(tab);
  }, [tab, load]);

  const runReconcile = async () => {
    setRunning(true);
    setError(null);
    try {
      const r = await apiPost<ReconciliationResult>("/api/reconciliations/run");
      setResult(r);
      load(tab);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  const act = async (id: number, action: "resolve" | "ignore") => {
    const note = window.prompt(action === "resolve" ? "처리 사유 (선택)" : "무시 사유 (선택)", "");
    if (note === null) return; // 취소
    setActingId(id);
    setError(null);
    try {
      await apiPost<Mismatch>(`/api/reconciliations/mismatches/${id}/${action}`, { note: note || null });
      load(tab);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setActingId(null);
    }
  };

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold">대사</h1>
          <p className="text-sm text-gray-500">우리 정산과 PG 리포트를 대조해 불일치를 찾고 처리합니다.</p>
        </div>
        <button
          onClick={runReconcile}
          disabled={running}
          className="rounded bg-gray-900 px-4 py-2 text-sm font-medium text-white hover:bg-gray-700 disabled:opacity-50"
        >
          {running ? "실행 중…" : "대사 실행"}
        </button>
      </div>

      {result && (
        <div className="mb-4 rounded border border-gray-200 bg-gray-50 px-4 py-3 text-sm text-gray-700">
          대사 완료 — 일치 <b>{result.matched}</b> · 신규 불일치 <b>{result.totalMismatches}</b>{" "}
          <span className="text-gray-500">
            (PG누락 {result.missingInPg} · 우리누락 {result.missingInOurs} · 금액 {result.amountMismatch} · 상태{" "}
            {result.statusMismatch})
          </span>{" "}
          · 이미 처리 {result.alreadyHandled}
        </div>
      )}
      {error && (
        <div className="mb-4 rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">{error}</div>
      )}

      {/* KPI — 직전 대사 결과 요약 */}
      <div className="mb-6 grid grid-cols-3 gap-4">
        <StatCard label="일치 (matched)" value={result ? `${result.matched}건` : "—"} accent="text-green-700" />
        <StatCard label="신규 불일치" value={result ? `${result.totalMismatches}건` : "—"} accent="text-red-600" />
        <StatCard label="이미 처리 (재대사 보존)" value={result ? `${result.alreadyHandled}건` : "—"} />
      </div>

      {/* 상태 필터 탭 */}
      <div className="mb-3 flex gap-1 border-b border-gray-200">
        {TABS.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`-mb-px border-b-2 px-3 py-2 text-sm ${
              tab === t.key
                ? "border-gray-900 font-medium text-gray-900"
                : "border-transparent text-gray-500 hover:text-gray-700"
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* 불일치 테이블 */}
      <div className="overflow-hidden rounded-lg border border-gray-200 bg-white">
        <table className="w-full text-sm">
          <thead className="bg-gray-50 text-left text-xs uppercase tracking-wide text-gray-500">
            <tr>
              <th className="px-4 py-3">유형</th>
              <th className="px-4 py-3">거래 ID</th>
              <th className="px-4 py-3 text-right">우리</th>
              <th className="px-4 py-3 text-right">PG</th>
              <th className="px-4 py-3">설명</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3 text-right">액션</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-gray-100">
            {loading ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
                  불러오는 중…
                </td>
              </tr>
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={7} className="px-4 py-8 text-center text-gray-400">
                  {tab === "OPEN" ? "미처리 불일치가 없습니다. “대사 실행”을 눌러보세요." : "항목이 없습니다."}
                </td>
              </tr>
            ) : (
              items.map((m) => (
                <tr key={m.id} className="hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs ${MISMATCH_TYPE_BADGE[m.type]}`}>
                      {MISMATCH_TYPE_LABEL[m.type]}
                    </span>
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-gray-500">{m.pgTransactionId.slice(0, 16)}…</td>
                  <td className="px-4 py-3 text-right">{won(m.ourAmount)}</td>
                  <td className="px-4 py-3 text-right">{won(m.pgAmount)}</td>
                  <td className="px-4 py-3 text-gray-600">{m.detail}</td>
                  <td className="px-4 py-3">
                    <span className={`rounded px-2 py-0.5 text-xs ${MISMATCH_STATUS_BADGE[m.status]}`}>
                      {MISMATCH_STATUS_LABEL[m.status]}
                    </span>
                    {m.resolutionNote && <span className="ml-1 text-xs text-gray-400">· {m.resolutionNote}</span>}
                  </td>
                  <td className="px-4 py-3 text-right">
                    {m.status === "OPEN" ? (
                      <span className="inline-flex gap-1">
                        <button
                          onClick={() => act(m.id, "resolve")}
                          disabled={actingId === m.id}
                          className="rounded border border-gray-300 px-2 py-1 text-xs hover:bg-gray-100 disabled:opacity-50"
                        >
                          처리
                        </button>
                        <button
                          onClick={() => act(m.id, "ignore")}
                          disabled={actingId === m.id}
                          className="rounded border border-gray-300 px-2 py-1 text-xs text-gray-500 hover:bg-gray-100 disabled:opacity-50"
                        >
                          무시
                        </button>
                      </span>
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
