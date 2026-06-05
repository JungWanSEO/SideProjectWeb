package com.commerce.api.product.repository;

import com.commerce.api.product.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 상품 DB 접근.
 *
 * <p>기본 CRUD는 {@link JpaRepository}가 제공한다.
 * 동적 검색/필터는 {@link ProductRepositoryCustom}(QueryDSL 구현)을 함께 상속해 사용한다.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, ProductRepositoryCustom {

    /**
     * 옵션 ID로 그 옵션이 속한 상품(애그리거트 루트)을 조회한다.
     * 주문 시 "루트 경유" 재고 차감에 사용 — 반환된 Product의 options에서 해당 옵션을 찾아 차감한다.
     */
    @Query("select p from Product p join p.options o where o.id = :optionId")
    Optional<Product> findByOptionId(@Param("optionId") Long optionId);
}
