/** 어드민 대시보드 KPI 카드 (정산·대사 화면 공통). */
export default function StatCard({
  label,
  value,
  accent,
}: {
  label: string;
  value: string;
  accent?: string; // 값 색상 (예: 수수료=amber, 실입금=green)
}) {
  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4">
      <div className="text-xs text-gray-500">{label}</div>
      <div className={`mt-1 text-xl font-bold ${accent ?? "text-gray-900"}`}>{value}</div>
    </div>
  );
}
