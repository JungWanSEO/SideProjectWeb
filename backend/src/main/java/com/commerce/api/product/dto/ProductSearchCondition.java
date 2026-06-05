package com.commerce.api.product.dto;

/**
 * 상품 검색/필터 조건.
 *
 * <p>모든 필드는 <b>선택적</b>이다 — null이면 그 조건은 적용하지 않는다.
 * 컨트롤러에서 쿼리 파라미터(`?keyword=..&minPrice=..&maxPrice=..&categoryId=..&brandId=..`)로 바인딩된다.
 *
 * @param keyword    상품명 부분 일치(LIKE %keyword%)
 * @param minPrice   이 가격 이상(price &gt;= minPrice)
 * @param maxPrice   이 가격 이하(price &lt;= maxPrice)
 * @param categoryId 이 카테고리만
 * @param brandId    이 브랜드만
 */
public record ProductSearchCondition(
        String keyword,
        Long minPrice,
        Long maxPrice,
        Long categoryId,
        Long brandId
) {
}
