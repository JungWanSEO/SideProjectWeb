package com.commerce.api.product.repository;

import com.commerce.api.product.dto.ProductSearchCondition;
import com.commerce.api.product.entity.Product;
import com.commerce.api.product.entity.ProductStatus;
import java.util.Collection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * QueryDSL로 직접 구현하는 커스텀 리포지토리.
 * (메서드 이름 규칙으로 만들기 어려운 "동적 조건" 쿼리를 담는다)
 *
 * <p><b>Spring Data 규칙(중요):</b> {@link ProductRepository}가 이 인터페이스를 함께 상속하고,
 * 구현 클래스 이름이 반드시 <b>{@code ProductRepositoryImpl}</b>(리포지토리 인터페이스명 + "Impl")
 * 이어야 스프링 데이터가 자동으로 찾아 끼워 넣는다. 이름 규칙을 어기면 연결되지 않는다.
 */
public interface ProductRepositoryCustom {

    /**
     * 가시 상태 + 검색 조건으로 상품을 페이지 조회한다.
     *
     * @param visibleStatuses 공개 목록에 노출할 상태(예: ON_SALE, SOLD_OUT) — 항상 적용
     * @param condition       선택적 검색 조건(키워드/가격대)
     * @param pageable        페이지 번호·크기·정렬
     */
    Page<Product> search(Collection<ProductStatus> visibleStatuses,
                         ProductSearchCondition condition,
                         Pageable pageable);
}
