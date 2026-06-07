// 주문 상태의 표시 라벨/뱃지 스타일을 한 곳에서 관리 (목록·상세·결제 화면 공통).
// 상태가 늘면 여기만 고치면 된다.
import { OrderStatus } from "./types";

export const ORDER_STATUS_LABEL: Record<OrderStatus, string> = {
  PENDING: "결제대기",
  PAID: "결제완료",
  CANCELLED: "취소됨",
};

// Tailwind 뱃지 클래스: 결제대기=주목(amber), 완료=강조(검정), 취소=흐림(회색)
export const ORDER_STATUS_BADGE: Record<OrderStatus, string> = {
  PENDING: "bg-amber-100 text-amber-700",
  PAID: "bg-gray-900 text-white",
  CANCELLED: "bg-gray-200 text-gray-500",
};
