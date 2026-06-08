// 대사 불일치 유형/상태의 표시 라벨·뱃지 (orderStatus.ts와 같은 패턴).
import { MismatchStatus, MismatchType } from "./types";

export const MISMATCH_TYPE_LABEL: Record<MismatchType, string> = {
  MISSING_IN_PG: "PG 누락",
  MISSING_IN_OURS: "우리 누락",
  AMOUNT_MISMATCH: "금액 상이",
  STATUS_MISMATCH: "상태 상이",
};

export const MISMATCH_TYPE_BADGE: Record<MismatchType, string> = {
  MISSING_IN_PG: "bg-orange-100 text-orange-700",
  MISSING_IN_OURS: "bg-purple-100 text-purple-700",
  AMOUNT_MISMATCH: "bg-red-100 text-red-700",
  STATUS_MISMATCH: "bg-rose-100 text-rose-700",
};

// 처리 상태: 미처리=주목(red), 처리됨=정상(green), 무시=흐림(gray)
export const MISMATCH_STATUS_LABEL: Record<MismatchStatus, string> = {
  OPEN: "미처리",
  RESOLVED: "처리됨",
  IGNORED: "무시",
};

export const MISMATCH_STATUS_BADGE: Record<MismatchStatus, string> = {
  OPEN: "bg-red-100 text-red-700",
  RESOLVED: "bg-green-100 text-green-700",
  IGNORED: "bg-gray-200 text-gray-500",
};
