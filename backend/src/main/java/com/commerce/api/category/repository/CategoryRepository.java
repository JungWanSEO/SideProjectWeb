package com.commerce.api.category.repository;

import com.commerce.api.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 카테고리 DB 접근.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 이름 중복 등록 방지용. 메서드명 규칙으로 {@code exists ... where name = ?} 쿼리 생성. */
    boolean existsByName(String name);
}
