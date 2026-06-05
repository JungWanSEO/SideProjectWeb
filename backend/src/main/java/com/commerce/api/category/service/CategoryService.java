package com.commerce.api.category.service;

import com.commerce.api.category.dto.CategoryCreateRequest;
import com.commerce.api.category.dto.CategoryResponse;
import com.commerce.api.category.entity.Category;
import com.commerce.api.category.repository.CategoryRepository;
import com.commerce.api.global.exception.BusinessException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카테고리 비즈니스 로직 — 목록 조회 / 등록.
 */
@Service
@RequiredArgsConstructor          // private final 필드를 받는 생성자 자동 생성(생성자 주입)
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** 전체 카테고리 목록(평면). 클라이언트 필터 UI 등에서 사용. */
    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream().map(CategoryResponse::from).toList();
    }

    /** 카테고리 등록(ADMIN). 이름이 이미 있으면 409. */
    @Transactional
    public CategoryResponse create(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new BusinessException(HttpStatus.CONFLICT, "이미 존재하는 카테고리입니다.");
        }
        return CategoryResponse.from(categoryRepository.save(Category.create(request.name())));
    }
}
