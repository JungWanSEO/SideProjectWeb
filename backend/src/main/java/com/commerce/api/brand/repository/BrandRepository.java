package com.commerce.api.brand.repository;

import com.commerce.api.brand.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 브랜드 DB 접근.
 */
public interface BrandRepository extends JpaRepository<Brand, Long> {

    /** 이름 중복 등록 방지용. */
    boolean existsByName(String name);
}
