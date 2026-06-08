// 정산 항목 상태의 표시 라벨/뱃지 스타일을 한 곳에서 관리 (orderStatus.ts와 같은 패턴).
import { SettlementStatus } from "./types";

export const SETTLEMENT_STATUS_LABEL: Record<SettlementStatus, string> = {
  SCHEDULED: "정산예정",
  PAID_OUT: "입금완료",
};

// 뱃지: 정산예정=대기(amber), 입금완료=정상(green)
export const SETTLEMENT_STATUS_BADGE: Record<SettlementStatus, string> = {
  SCHEDULED: "bg-amber-100 text-amber-700",
  PAID_OUT: "bg-green-100 text-green-700",
};
