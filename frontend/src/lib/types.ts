// 백엔드(Spring Boot) 응답 계약을 TypeScript 타입으로. (Swagger 기준)
// 백엔드 DTO가 바뀌면 여기도 같이 맞춘다.

/** 공통 응답 포맷: { success, message, data } */
export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
}

/** 페이지 응답 (PageResponse<T>) */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
}

export type ProductStatus = "ON_SALE" | "SOLD_OUT" | "DISCONTINUED";

/** 상품 옵션(사이즈) — 재고/품절은 옵션 단위 */
export interface ProductOption {
  id: number;
  size: string;
  stock: number;
  soldOut: boolean;
}

/** 상품 (ProductResponse) */
export interface Product {
  id: number;
  name: string;
  price: number;
  description: string | null;
  status: ProductStatus;
  categoryId: number | null;
  categoryName: string | null;
  brandId: number | null;
  brandName: string | null;
  options: ProductOption[];
  createdAt: string;
}

/** 장바구니 항목 (CartItemResponse) — size·stock·soldOut은 현재(라이브) 옵션 정보 */
export interface CartItem {
  productId: number;
  optionId: number;
  productName: string;
  size: string;
  price: number;
  quantity: number;
  subtotal: number;
  stock: number;
  soldOut: boolean;
}

/** 장바구니 (CartResponse) */
export interface Cart {
  memberId: number;
  items: CartItem[];
  totalQuantity: number;
}

// 결제 도입 후: 주문은 결제 대기(PENDING) → 결제 완료(PAID) / 취소(CANCELLED)
export type OrderStatus = "PENDING" | "PAID" | "CANCELLED";

// 결제 상태머신 (백엔드 PaymentStatus). READY→PAID/FAILED, PAID→CANCELLED(환불)
export type PaymentStatus = "READY" | "PAID" | "FAILED" | "CANCELLED";

/** 결제 (PaymentResponse) */
export interface Payment {
  id: number;
  orderId: number;
  amount: number;
  status: PaymentStatus;
  method: string;
  provider: string; // 실제 승인한 PG (TOSS/KAKAOPAY) — 페일오버 시 요청과 다를 수 있음
  pgTransactionId: string | null; // 승인 성공 시에만 채워짐
  createdAt: string;
}

/** 주문 항목 (OrderItemResponse) — 주문 시점 스냅샷(상품명·사이즈·가격) */
export interface OrderItem {
  productId: number;
  optionId: number;
  productName: string;
  size: string;
  orderPrice: number;
  quantity: number;
  subtotal: number;
}

/** 주문 상세 (OrderResponse) */
export interface Order {
  id: number;
  memberId: number;
  status: OrderStatus;
  totalPrice: number;
  items: OrderItem[];
  createdAt: string;
}

/** 주문 목록 요약 (OrderSummaryResponse) — 목록은 가볍게(대표상품명 + 항목수) */
export interface OrderSummary {
  id: number;
  status: OrderStatus;
  totalPrice: number;
  createdAt: string;
  representativeProductName: string;
  itemCount: number;
}

// ───────── 정산(Settlement) — ADMIN 운영 ─────────

// 정산 항목 상태 (백엔드 SettlementStatus). 정산예정 → 입금완료
export type SettlementStatus = "SCHEDULED" | "PAID_OUT";

/** 정산 항목 (SettlementResponse) — 매출 ≠ 결제액: gross/fee/net 분리 */
export interface Settlement {
  id: number;
  paymentId: number;
  orderId: number;
  pgTransactionId: string; // 대사 조인 키
  provider: string; // 정산 대상 결제를 처리한 PG (MPG-2)
  grossAmount: number; // 결제액
  fee: number; // 수수료
  feeRate: number; // 적용 수수료율 스냅샷 (예: 0.025) — MPG-3
  netAmount: number; // 실입금 (= gross - fee)
  status: SettlementStatus;
  settledDate: string; // 입금 예정/완료일 (LocalDate "YYYY-MM-DD")
  createdAt: string;
}

/** 정산 배치 결과의 PG별 분해 (SettlementRunResponse.ProviderBreakdown) — MPG-3 */
export interface SettlementProviderBreakdown {
  provider: string;
  feeRate: number;
  count: number;
  grossAmount: number;
  fee: number;
  netAmount: number;
}

/** 정산 배치 실행 결과 (SettlementRunResponse) */
export interface SettlementRunResult {
  createdCount: number;
  totalGrossAmount: number;
  totalFee: number;
  totalNetAmount: number;
  byProvider: SettlementProviderBreakdown[]; // PG별 분해 — MPG-3
}

// ───────── 대사(Reconciliation) — ADMIN 운영 ─────────

// 불일치 유형 (백엔드 MismatchType)
export type MismatchType = "MISSING_IN_PG" | "MISSING_IN_OURS" | "AMOUNT_MISMATCH" | "STATUS_MISMATCH";

// 불일치 처리 상태 (백엔드 MismatchStatus). 미처리 → 처리됨/무시
export type MismatchStatus = "OPEN" | "RESOLVED" | "IGNORED";

/** 대사 불일치 항목 (MismatchResponse). ourAmount/pgAmount는 한쪽에만 있으면 null */
export interface Mismatch {
  id: number;
  pgTransactionId: string;
  provider: string; // 어느 PG의 거래인가 (MPG-2)
  type: MismatchType;
  ourAmount: number | null;
  pgAmount: number | null;
  detail: string;
  status: MismatchStatus;
  resolutionNote: string | null;
  createdAt: string;
}

/** 대사 결과의 PG별 분해 (ReconciliationResult.ProviderReconciliation) — MPG-2 */
export interface ReconciliationProviderBreakdown {
  provider: string;
  matched: number;
  missingInPg: number;
  missingInOurs: number;
  amountMismatch: number;
  statusMismatch: number;
  totalMismatches: number;
  alreadyHandled: number;
}

/** 대사 실행 결과 요약 (ReconciliationResult) */
export interface ReconciliationResult {
  matched: number;
  missingInPg: number;
  missingInOurs: number;
  amountMismatch: number;
  statusMismatch: number;
  totalMismatches: number;
  alreadyHandled: number;
  byProvider: ReconciliationProviderBreakdown[]; // PG별 분해 — MPG-2
}
