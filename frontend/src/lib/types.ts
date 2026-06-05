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

export type OrderStatus = "ORDERED" | "CANCELLED";

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
